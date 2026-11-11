# Ambiente local de mensageria

Requer Docker Compose. `make dev-up` sobe LocalStack 4.9.0 e o serviço com perfil `dev`. Sem Make, use `docker compose -f dev/integration/compose.yml up -d --build app`.

O perfil dev usa credenciais locais `test`, endpoints explícitos e não depende da conta AWS. No host, LocalStack está em `http://localhost:14566`; entre containers, em `http://localstack:4566`. Não usar esse perfil ou o segredo de desenvolvimento em ambientes compartilhados. A API do topo da stack é exposta somente no loopback em `http://localhost:18081/api/v1/notifications`.

O bootstrap cria a tabela Notifications, os índices TypeIndex/ReferenceIdIndex, a fila `ofisy-notification.fifo`, sua DLQ `ofisy-notification-dlq.fifo` e assinaturas raw com filtro por `type` no corpo:

| Tópico SNS FIFO | Evento externo |
|---|---|
| ofisy-stock-events.fifo | StockLow |
| ofisy-billing-events.fifo | QuoteCreated |

`serviceOrderId` fica no envelope, sem duplicação no payload de QuoteCreated. Os tipos internos/persistidos LOW_STOCK e QUOTE_GENERATED continuam iguais aos do core. Eventos sem dados adicionais podem usar payload `{}`; os eventos reconhecidos acima exigem seus campos específicos.

`make dev-test` valida SNS → SQS → aplicação → DynamoDB → API HTTP real, os dois contratos, persistência, duplicatas, filtros, retry, DLQ, progresso de outro grupo e replay. A saída deve conter `"status": "PASS"`. `make dev-test-messaging` executa somente o smoke de mensageria, também disponível em #21. Cada execução deve começar com ambiente limpo, via `make dev-down`, para não misturar mensagens da DLQ.

Comandos de cenários: `make dev-stock`, `make dev-quote`, `make dev-invalid`, `make dev-status` e `make dev-replay`. O último reenvia uma mensagem da DLQ preservando eventId, reparando apenas o campo removido pelo cenário invalid. Equivalente sem Make: `docker compose -f dev/integration/compose.yml run --rm --build --no-deps smoke python /tools/events.py stock`.

`make dev-down` encerra somente este projeto Compose. Não há volume persistente: o objetivo é um ambiente descartável. Recursos AWS reais, Terraform, IAM e alarmes continuam sob responsabilidade do repositório de infraestrutura.

## API e contrato do core

JWT de funcionário segue o core: `iss=techchallenge-ofisy-api`, `sub` com o usuário, `role` singular (ADMIN, STOCKMAN, ATTENDANT ou MECHANIC), assinatura e expiração válidas. Estoque exige ADMIN/STOCKMAN. As rotas de notificações de OS mantêm o acesso público anterior, inclusive PATCH; qualquer mudança nessa política exige decisão separada. O segredo local padrão é público e exclusivo de desenvolvimento.

As quatro listagens mantêm `limit=50` como padrão e agora aceitam somente 1–100. Entrada inválida retorna HTTP 400 com ProblemDetail. O `nextToken` é opaco, versionado e específico da categoria e do filtro (todas/não lidas); tokens antigos, malformados ou de outra consulta retornam 400, em vez de reiniciar silenciosamente na primeira página. Essa é uma mudança deliberada de contrato antes de produção. Páginas vazias com nextToken continuam possíveis para consultas filtradas; percorra até nextToken ser null.

Para a suíte Java no PowerShell: `$env:DYNAMODB_TEST_ENDPOINT='http://localhost:14566'; mvn clean verify`, seguido de `python dev/integration/assert_reports.py`. No Bash: `DYNAMODB_TEST_ENDPOINT=http://localhost:14566 mvn clean verify`. O workflow provisiona LocalStack, exige execução sem skips das integrações obrigatórias e executa o smoke HTTP completo. Um CI verde apenas com testes unitários não basta.

## Limites desta entrega

Regras de mensagem, categorias internas, referências, estado de leitura e 404 para tipo incorreto permanecem equivalentes ao core. O listener apenas traduz os nomes externos. Eventos adicionais desenhados para notification precisam de matriz evento/regra/schema aprovada; esta entrega cobre StockLow e QuoteCreated. Exemplos do desenho com type divergente do título não devem ser copiados literalmente. Terraform, IAM, alarmes, integração real com publicadores e a política de acesso via Gateway são dependências próprias, não comprovadas por LocalStack.
