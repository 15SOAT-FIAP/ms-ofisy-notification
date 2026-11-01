# Ambiente local de mensageria

Requer Docker Compose. `make dev-up` sobe LocalStack 4.9.0 e o serviço com perfil `dev`. Sem Make, use `docker compose -f dev/integration/compose.yml up -d --build app`.

O perfil dev usa credenciais locais `test`, endpoints explícitos e não depende da conta AWS. No host, LocalStack está em `http://localhost:14566`; entre containers, em `http://localstack:4566`. Não usar esse perfil ou o segredo de desenvolvimento em ambientes compartilhados. A API do topo da stack é exposta somente no loopback em `http://localhost:18081/api/v1/notifications`.

O bootstrap cria a tabela Notifications, os índices TypeIndex/ReferenceIdIndex, a fila `ofisy-notification.fifo`, sua DLQ `ofisy-notification-dlq.fifo` e assinaturas raw com filtro por `type` no corpo:

| Tópico SNS FIFO | Evento externo |
|---|---|
| ofisy-stock-events.fifo | StockLow |
| ofisy-billing-events.fifo | QuoteSentForApproval |

`serviceOrderId` fica no envelope, sem duplicação no payload de QuoteSentForApproval. Os tipos internos/persistidos LOW_STOCK e QUOTE_GENERATED continuam iguais aos do core. Eventos sem dados adicionais podem usar payload `{}`; os eventos reconhecidos acima exigem seus campos específicos.

`make dev-test` valida SNS → SQS → aplicação → DynamoDB → API HTTP real, os dois contratos, persistência, duplicatas, filtros, retry, DLQ, progresso de outro grupo e replay. A saída deve conter `"status": "PASS"`. `make dev-test-messaging` executa somente o smoke de mensageria, sem as verificações HTTP. Cada execução deve começar com ambiente limpo, via `make dev-down`, para não misturar mensagens da DLQ.

Comandos de cenários: `make dev-stock`, `make dev-quote` (QuoteSentForApproval), `make dev-quote-created` e `make dev-stock-failure` (eventos que não devem criar aviso de orçamento), `make dev-invalid`, `make dev-status` e `make dev-replay`. O último reenvia uma mensagem da DLQ preservando eventId, reparando apenas o campo removido pelo cenário invalid. Equivalente sem Make: `docker compose -f dev/integration/compose.yml run --rm --build --no-deps smoke python /tools/events.py stock`.

`make dev-down` encerra somente este projeto Compose. Não há volume persistente: o objetivo é um ambiente descartável. Recursos AWS reais, Terraform, IAM e alarmes continuam sob responsabilidade do repositório de infraestrutura.

## API e contrato do core

JWT de funcionário segue o core: `iss=techchallenge-ofisy-api`, `sub` com o usuário, `role` singular (ADMIN, STOCKMAN, ATTENDANT ou MECHANIC), assinatura e expiração válidas. Estoque exige ADMIN/STOCKMAN. As rotas de notificações de OS mantêm o acesso público anterior, inclusive PATCH; qualquer mudança nessa política exige decisão separada. O segredo local padrão é público e exclusivo de desenvolvimento.

As quatro listagens mantêm `limit=50` como padrão e agora aceitam somente 1–100. Entrada inválida retorna HTTP 400 com ProblemDetail. O `nextToken` é opaco, versionado e específico da categoria e do filtro (todas/não lidas); tokens antigos, malformados ou de outra consulta retornam 400, em vez de reiniciar silenciosamente na primeira página. Essa é uma mudança deliberada de contrato antes de produção. Páginas vazias com nextToken continuam possíveis para consultas filtradas; percorra até nextToken ser null.

Para a suíte Java no PowerShell: `$env:DYNAMODB_TEST_ENDPOINT='http://localhost:14566'; mvn clean verify`, seguido de `python dev/integration/assert_reports.py`. No Bash: `DYNAMODB_TEST_ENDPOINT=http://localhost:14566 mvn clean verify`. O workflow provisiona LocalStack, exige execução sem skips das integrações obrigatórias e executa o smoke HTTP completo. Um CI verde apenas com testes unitários não basta.

## Limites desta entrega

Categorias internas, referências, estado de leitura e 404 para tipo incorreto permanecem equivalentes ao core. A entrada por evento de orçamento agora produz a mensagem de disponibilidade para aprovação no domínio. A entrada legada execute(command) de orçamento e a mensagem de orçamento gerado foram descomissionadas; a criação de orçamento exige eventId para deduplicação. O listener continua traduzindo transporte em commands. Eventos adicionais desenhados para notification precisam de matriz evento/regra/schema aprovada; esta entrega cobre StockLow e QuoteSentForApproval. Exemplos do desenho com type divergente do título não devem ser copiados literalmente. Terraform, IAM, alarmes, integração real com publicadores e a política de acesso via Gateway são dependências próprias, não comprovadas por LocalStack.

## Orçamento disponível para aprovação

O aviso de orçamento é criado somente ao consumir `QuoteSentForApproval`, após a etapa de estoque no fluxo de Billing. `QuoteCreated` continua sendo um evento de Billing para Stock e é excluído da assinatura de Notification. Sua entrega direta ao listener também não cria notificação. O fluxo StockLow permanece igual.

O consumidor espera eventId/type/serviceOrderId/occurredAt no envelope e quoteId/totalPrice no payload. O serviceOrderId não é duplicado no payload. Esse contrato usa os dados necessários ao consumidor e ainda precisa ser conferido com o publicador real de Billing, que não estava implementado nos heads consultados de seus PRs em 01/11/2026. Os cenários locais simulam o publicador; não validam a transação real de consumo de estoque do outro serviço.

O smoke publica QuoteCreated sem notificação, simula falha de estoque sem envio para aprovação e usa QuoteSentForApproval como controle positivo. Também entrega QuoteCreated diretamente na SQS, exercita o evento novo sem atributos SNS redundantes e valida payload inválido de aprovação → retry → DLQ → reparo/replay. O texto esperado é “Orçamento #{quoteId} disponível para aprovação na ordem de serviço '{serviceOrderId}'. Valor total: R$ {valor com duas casas}”. O tipo persistido/API QUOTE_GENERATED e o schema DynamoDB são mantidos.

A assinatura AWS de Billing para Notification também precisa mudar seu filtro para QuoteSentForApproval de forma coordenada com a implantação do consumidor e o publicador. O bootstrap altera somente LocalStack. Core e Notification são consumidores independentes: o recebimento do evento não garante que o Core já tenha atualizado o status da OS. A notificação não consulta outro banco/serviço para aguardar essa atualização.
