# Notificar orçamento disponível para aprovação

Data: 01/11/2026. Status: especificação para implementação; o gatilho foi alinhado entre Tetheugas e Roger. O schema do publicador ainda precisa de confirmação. Este PR contém somente a especificação e não muda o comportamento da aplicação.

## Problema e resultado esperado

Hoje notification cria o aviso de orçamento ao consumir QuoteCreated. Na coreografia do time, esse evento inicia o consumo de estoque. A etapa pode falhar e levar ao cancelamento antes de o orçamento ficar disponível para o cliente.

A notificação de orçamento deve ser criada ao consumir **QuoteSentForApproval**, emitido por Billing quando o orçamento estiver disponível para aprovação após o sucesso da etapa de estoque. QuoteCreated continua existindo no sistema como gatilho de Stock, mas deixa de criar esse aviso em notification.

Essa é uma mudança deliberada de comportamento, acordada para refletir o fluxo do Miro. Não decorre de uma necessidade técnica de DynamoDB/SQS. O sucesso do estoque não impede cancelamentos ou reprovações posteriores; esta alteração não pretende garantir que o orçamento nunca será cancelado.

## Referências e fotografia da stack

- Base deste PR: [#23](https://github.com/15SOAT-FIAP/ms-ofisy-notification/pull/23), branch stack/06-rest-api, SHA `8d6cba50dfcd03ff5c8fed07dc17ad407a0bcbb3`.
- Proposta do time: [board do Miro](https://miro.com/app/board/uXjVGnrcchs=/), exportação DDD - Tech Challenge (2).pdf fornecida em 31/10/2026, especialmente página 10. O acordo posterior entre Tetheugas/Roger define a troca do gatilho de notification.
- Fluxo de referência: Billing → QuoteCreated → Stock → StockConsumed → Billing → QuoteSentForApproval → consumidores, incluindo Core e Notification.
- ADR-0002/0010/0011/0012/0013: fronteiras, coreografia, envelope comum e persistência idempotente.
- Billing consultado em 01/11/2026: [PR #1](https://github.com/15SOAT-FIAP/ms-ofisy-billing/pull/1), SHA `2abe30eccc0ceb5432ef79b40cfc58b75259e16c`, e [PR #2](https://github.com/15SOAT-FIAP/ms-ofisy-billing/pull/2), SHA `29fc51de4ab15566d708bd68ec2b7dcc4cefa98c`. Não foi localizada implementação de publicação de eventos nesses heads. O JSON abaixo é contrato alvo proposto para alinhamento, não schema comprovado do produtor.

## Escopo

- Alterar o consumo de notification de QuoteCreated para QuoteSentForApproval.
- Alinhar DTO de transporte, validação, mensagem e logs ao novo significado.
- Alterar a assinatura/filtro SNS de Billing no ambiente local e definir a alteração correspondente na infraestrutura AWS responsável.
- Atualizar cenários, testes, documentação e descrições que tratem do evento consumido por notification.
- Implementar tudo em branch acima do #23, preservando o histórico e o conteúdo dos PRs inferiores.

Permanecem iguais: fluxo StockLow, categorias internas e formato da resposta HTTP, autorização, paginação, schema DynamoDB, ID da notificação por eventId, inserção condicional e preservação de dados em redelivery. Sem implementar produtores de Billing/Stock, novas notificações de compensação, e-mail/WhatsApp ou mudanças de infraestrutura de banco nesta entrega.

## Contrato de transporte

| Elemento | Contrato alvo |
|---|---|
| Publicador | MS Billing |
| Tópico SNS FIFO | ofisy-billing-events.fifo |
| Evento / campo type | QuoteSentForApproval, com essa grafia |
| Fila consumidora | ofisy-notification.fifo |
| DLQ | ofisy-notification-dlq.fifo |
| Envelope | eventId UUID, type, serviceOrderId UUID, occurredAt Instant e payload objeto |
| Campos propostos do payload | quoteId UUID e totalPrice decimal; confirmar com Billing antes da implementação do contrato final |
| Fonte de serviceOrderId | Somente envelope, sem duplicação no payload |
| Agrupamento do produtor | MessageGroupId pela OS |
| Idempotência | eventId estável para reentrega do mesmo evento lógico |

Exemplo alvo, usando os campos que o command atual de notification já necessita:

```json
{
  "eventId": "7e0e9958-c573-450a-96ea-fdc6a752d726",
  "type": "QuoteSentForApproval",
  "serviceOrderId": "7c2780dd-3a48-4aca-bb75-508ed9f4fe52",
  "occurredAt": "2026-11-01T18:00:00Z",
  "payload": {
    "quoteId": "50dce245-9a5c-465d-a399-46aab8dc2064",
    "totalPrice": 123.45
  }
}
```

Confirmar quoteId, totalPrice, obrigatoriedade e tipo monetário com Billing. Se o evento real não trouxer o valor total, acordar a adaptação da mensagem/command ou do produtor antes de finalizar a implementação; não consultar o banco de Billing ou depender de chamada síncrona ao Core para completar o evento.

O envelope genérico pode representar eventos com payload vazio `{}`. Isso não elimina os campos necessários para este evento específico: no contrato alvo acima, ausência de quoteId, totalPrice ou serviceOrderId impede criar a notificação e deve causar falha de processamento/retry/DLQ conforme a política existente.

## Regras de comportamento

1. Ao receber QuoteSentForApproval válido, traduzir o transporte para o caso de uso de orçamento e criar uma notificação usando eventId como ID, quoteId como referência e a mensagem de disponibilidade para aprovação.
2. QuoteCreated isolado não cria notificação de orçamento. Seu recebimento direto inesperado pode seguir o tratamento atual de tipo não suportado; o filtro SNS deve impedir seu encaminhamento normal a notification.
3. Ausência de QuoteSentForApproval após falha de estoque resulta em ausência desse aviso. Não criar notificação positiva a partir de StockConsumptionFailed, QuoteCancelled ou mero QuoteCreated.
4. Redelivery com o mesmo eventId mantém leitura, mensagem, referências e timestamps do primeiro registro. Falhas de persistência que não sejam duplicatas continuam propagadas para retry.
5. StockLow mantém seu comportamento e sua assinatura.
6. Core e Notification consomem de forma independente. O aviso não depende de o Core já ter atualizado a OS para AWAITING_APPROVAL; não há garantia de ordem global entre essas filas. Se a interface exigir leitura imediata desse estado, tratar a consistência eventual na integração própria.
7. Esta entrega não define múltiplos envios para aprovação de um mesmo orçamento nem deduplicação entre eventIds diferentes. Se Billing publicar um novo evento lógico de reenvio/versão, alinhar a semântica em decisão específica antes de ampliar a regra atual.

## Mensagem e preservação dos contratos do core

Texto proposto para a entrada por evento:

> Orçamento #{quoteId} disponível para aprovação na ordem de serviço '{serviceOrderId}'. Valor total: R$ {totalPrice com duas casas}

Preservar os limites e o formato numérico já utilizados pela aplicação; mudança de localização monetária não faz parte desta entrega. A composição da mensagem permanece no domínio, e não no listener.

O service atual usa NotificationMessage.forQuote tanto em execute(command) quanto em executeEvent(eventId, command). A implementação deve distinguir a mensagem de disponibilidade para aprovação na entrada por evento, preservando o comportamento legado de execute(command), salvo aprovação explícita de alteração adicional. Uma fábrica de mensagem específica no domínio é uma opção compatível com essas fronteiras.

Os nomes internos CreateQuoteNotificationUseCase/CreateQuoteCommand e o tipo persistido/API QUOTE_GENERATED podem permanecer para evitar mudança desnecessária de contrato. Não renomear globalmente o enum ou os registros existentes ao trocar o nome do evento externo. Nenhuma migração de dados DynamoDB é necessária para a proposta descrita.

## Plano de implementação

| Área | Alteração prevista |
|---|---|
| adapters/listeners/notification/NotificationEventListener.java | Trocar o caso consumido por QuoteSentForApproval; manter tradução, validação específica, logs e propagação de falhas. |
| adapters/listeners/dto/QuoteGeneratedPayload.java | Alinhar o DTO ao schema acordado; adotar nome de transporte coerente, como QuoteSentForApprovalPayload. Não mover DTO para domínio/aplicação. |
| application/notification/createquote | Preservar o caso de uso e sua idempotência; aplicar a mensagem apropriada na entrada por evento. |
| domain/notification/NotificationMessage.java | Centralizar o texto de orçamento disponível para aprovação, preservando a fábrica/semântica legada quando necessária. |
| dev/integration/bootstrap.py | Assinatura de ofisy-billing-events.fifo com filtro type=QuoteSentForApproval no corpo; manter StockLow e raw delivery. |
| dev/integration/events.py | Cenário quote publica QuoteSentForApproval, com contrato acordado; manter serviceOrderId só no envelope. |
| dev/integration/smoke_messaging.py e smoke.py | Atualizar evento, mensagem esperada, controles de filtro e cenários de ausência de aviso prematuro. |
| Testes Java | Cobrir roteamento, domínio/mensagem, preservação do command legado, validação e idempotência. |
| Documentação | Atualizar dev/integration/README.md e demais referências ao evento de orçamento consumido. Exemplos de QuoteCreated como evento do Stock continuam válidos. |
| Infraestrutura AWS responsável | Ajustar assinatura SNS/filtro por corpo da fila notification de forma coordenada com aplicação e publicador. O bootstrap local não altera a AWS real. |

Não alterar os PRs #20–#23 para esta nova decisão de negócio. A branch deste PR parte do #23. Se a base avançar, sincronizar e revalidar o diff antes de publicar a implementação.

## Critérios de aceite e validação

| Cenário | Resultado esperado |
|---|---|
| QuoteSentForApproval válido | Uma notificação de orçamento com referência e mensagem corretas. |
| QuoteCreated enviado diretamente ao listener | Nenhuma chamada de criação de orçamento. |
| Publicar apenas QuoteCreated pelo SNS | Sem notificação; usar controle positivo no mesmo cenário para comprovar filtro ativo. |
| Publicar QuoteSentForApproval sem atributos SNS redundantes | Filtro por corpo encaminha e a aplicação persiste a notificação. |
| Fluxo simulado de falha de estoque, sem QuoteSentForApproval | Nenhum aviso de orçamento disponível para aprovação. Não equivale a provar o produtor real. |
| QuoteSentForApproval sem campo obrigatório do contrato acordado | Falha sem criação; retries e DLQ mantidos. |
| Redelivery após marcar leitura | read e updatedAt permanecem iguais; nenhuma segunda criação. |
| Erro de infraestrutura | Propagado; não tratado como duplicata/sucesso. |
| StockLow | Criação, mensagem e redelivery continuam iguais. |
| execute(command) legado | Comportamento e mensagem existentes preservados, sem mudança silenciosa. |
| API HTTP | Tipo interno, rotas/campos, categorias/404, autorização e paginação preservados. |

Na implementação: executar testes unitários e a suíte completa com DYNAMODB_TEST_ENDPOINT local, exigir integrações obrigatórias sem skips e rodar os smokes SNS → SQS → aplicação → DynamoDB → HTTP em ambiente limpo. Verificar gates de cobertura existentes sem reduzi-los; registrar resultados por SHA e confirmar CI remoto no head publicado. O total histórico de 89 testes não é uma meta fixa.

Nesta etapa de documentação, validar o JSON de exemplo, os links/arquivos referenciados e o diff do PR. Não apresentar critérios de aceite como testes já executados da nova regra.

## Coordenação de entrega e pendências

- [ ] Confirmar o payload QuoteSentForApproval e o momento de emissão com Billing.
- [ ] Alinhar o board/contrato de assinatura de Notification, registrando a decisão de consumo.
- [ ] Implementar listener/DTO/mensagem sem alterar regras ou contratos fora deste escopo.
- [ ] Atualizar assinatura local e a entrega correspondente na infraestrutura AWS.
- [ ] Executar testes e smokes positivos/negativos, incluindo idempotência.
- [ ] Validar contrato com publicador real quando disponível.

Coordenar ativação do publicador, filtro e consumidor. Atualizar um antes dos outros sem planejamento pode filtrar mensagens ou confirmá-las como tipo desconhecido. Como o projeto ainda estava em desenvolvimento nesta conversa, não adicionar aceitação dupla de QuoteCreated por hábito: isso reintroduziria o aviso prematuro que se pretende eliminar. Confirmar o estado real de implantação antes de definir a sequência operacional.
