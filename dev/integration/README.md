# Ambiente local de mensageria

Requer Docker Compose. `make dev-up` sobe LocalStack 4.9.0 e o serviço com perfil `dev`. Sem Make, use `docker compose -f dev/integration/compose.yml up -d --build app`.

O perfil dev usa credenciais locais `test`, endpoints explícitos e não depende da conta AWS. No host, LocalStack está em `http://localhost:14566`; entre containers, em `http://localstack:4566`. Não usar esse perfil ou o segredo de desenvolvimento em ambientes compartilhados. A API do topo da stack é exposta somente no loopback em `http://localhost:18081/api/v1/notifications`.

O bootstrap cria a tabela Notifications, os índices TypeIndex/ReferenceIdIndex, a fila `ofisy-notification.fifo`, sua DLQ `ofisy-notification-dlq.fifo` e assinaturas raw com filtro por `type` no corpo:

| Tópico SNS FIFO | Evento externo |
|---|---|
| ofisy-stock-events.fifo | StockLow |
| ofisy-billing-events.fifo | QuoteCreated |

`serviceOrderId` fica no envelope, sem duplicação no payload de QuoteCreated. Os tipos internos/persistidos LOW_STOCK e QUOTE_GENERATED continuam iguais aos do core. Eventos sem dados adicionais podem usar payload `{}`; os eventos reconhecidos acima exigem seus campos específicos.

`make dev-test` valida os dois contratos, persistência, duplicatas, filtros, retry, DLQ, progresso de outro grupo e replay. A saída deve conter `"status": "PASS"`. Cada execução deve começar com ambiente limpo, via `make dev-down`, para não misturar mensagens da DLQ.

Comandos de cenários: `make dev-stock`, `make dev-quote`, `make dev-invalid`, `make dev-status` e `make dev-replay`. O último reenvia uma mensagem da DLQ preservando eventId, reparando apenas o campo removido pelo cenário invalid. Equivalente sem Make: `docker compose -f dev/integration/compose.yml run --rm --build --no-deps smoke python /tools/events.py stock`.

`make dev-down` encerra somente este projeto Compose. Não há volume persistente: o objetivo é um ambiente descartável. Recursos AWS reais, Terraform, IAM e alarmes continuam sob responsabilidade do repositório de infraestrutura.
