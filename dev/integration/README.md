# Ambiente local de mensageria

Requer Docker Compose. `make dev-up` sobe LocalStack 4.9.0 e o serviço com perfil `dev`. Sem Make, use `docker compose -f dev/integration/compose.yml up -d --build app`.

O perfil dev usa credenciais locais `test`, endpoints explícitos e não depende da conta AWS. No host, LocalStack está em `http://localhost:14566`; entre containers, em `http://localstack:4566`. Não usar esse perfil ou o segredo de desenvolvimento em ambientes compartilhados. A API do topo da stack é exposta somente no loopback em `http://localhost:18081/api/v1/notifications`.

O script `init-aws.sh` cria a tabela Notifications, os índices TypeIndex, ReferenceIdIndex e CustomerIdIndex, a fila `ofisy-notification.fifo`, sua DLQ `ofisy-notification-dlq.fifo` e assinaturas raw com filtro por `type` no corpo:

| Tópico SNS FIFO | Evento externo |
|---|---|
| ofisy-stock-events.fifo | StockLow |
| ofisy-billing-events.fifo | QuoteSentForApproval, PaymentCheckoutCreated, PaymentConfirmed, PaymentFailed |

`serviceOrderId` fica no envelope, sem duplicação no payload de QuoteSentForApproval. Os tipos internos/persistidos LOW_STOCK e QUOTE_GENERATED continuam iguais aos do core. Eventos sem dados adicionais podem usar payload `{}`; os eventos reconhecidos acima exigem seus campos específicos.

`make dev-test` executa a suíte Maven com LocalStack. `make dev-test-messaging` executa somente o teste Java de mensageria com Testcontainers.

Comandos de cenários: `make dev-stock`, `make dev-quote` (QuoteSentForApproval), `make dev-quote-created` e `make dev-stock-failure` (eventos que não devem criar aviso de orçamento), `make dev-invalid`, `make dev-status` e `make dev-replay`. O último reenvia uma mensagem da DLQ preservando eventId, reparando apenas o campo removido pelo cenário invalid. Equivalente sem Make: `docker compose -f dev/integration/compose.yml run --rm --build --no-deps tools stock`.

`make dev-down` encerra somente este projeto Compose. Não há volume persistente: o objetivo é um ambiente descartável. Recursos AWS reais, Terraform, IAM e alarmes continuam sob responsabilidade do repositório de infraestrutura.

## API e contrato do core

O JWT de funcionário segue o Core, com issuer `techchallenge-ofisy-api`, subject do
usuário e claim `role` singular. Estoque exige ADMIN ou STOCKMAN. As quatro rotas de
notificação de OS recebem o `X-Customer-Id` obrigatório do Gateway, que valida o
JWT de cliente. Para processar avisos de orçamento no Compose, configure
`APP_CORE_BASE_URL` e `APP_CORE_SERVICE_TOKEN` de um Core local acessível ao container
e execute `make dev-quote SERVICE_ORDER_ID=<UUID-da-OS>`.

As quatro listagens mantêm `limit=50` como padrão e agora aceitam somente 1–100. Entrada inválida retorna HTTP 400 com ProblemDetail. O `nextToken` é opaco, versionado e específico da categoria e do filtro (todas/não lidas); tokens antigos, malformados ou de outra consulta retornam 400, em vez de reiniciar silenciosamente na primeira página. Essa é uma mudança deliberada de contrato antes de produção. Páginas vazias com nextToken continuam possíveis para consultas filtradas; percorra até nextToken ser null.

Para a suíte Java no PowerShell: `$env:DYNAMODB_TEST_ENDPOINT='http://localhost:14566'; mvn clean verify`, seguido de `python dev/integration/assert_reports.py`. No Bash: `DYNAMODB_TEST_ENDPOINT=http://localhost:14566 mvn clean verify`. O workflow também provisiona LocalStack e exige a execução das integrações obrigatórias.

## Escopo

Categorias internas, referências, estado de leitura e 404 para tipo incorreto permanecem equivalentes ao monolito de onde o serviço foi extraído. A entrada legada execute(command) de orçamento e a mensagem de orçamento gerado foram descomissionadas; a criação por evento exige eventId para deduplicação. O listener continua traduzindo transporte em commands. O serviço cobre StockLow, QuoteSentForApproval, PaymentCheckoutCreated, PaymentConfirmed e PaymentFailed. Terraform, IAM e a política de acesso via Gateway ficam no `techchallenge-ofisy-eks-infra` e não são comprovados por LocalStack.

## Avisos de orçamento e de pagamento

O aviso de orçamento é criado somente ao consumir `QuoteSentForApproval`, após a etapa de estoque no fluxo de Billing. `QuoteCreated` é um evento de Billing para Stock e fica fora da assinatura de Notification. Sua entrega direta ao listener também não cria notificação. Um orçamento editado volta a ser enviado para aprovação, e cada `QuoteSentForApproval` (com seu próprio eventId) gera um novo aviso com o total atualizado.

O consumidor espera eventId/type/serviceOrderId/occurredAt no envelope. Os payloads seguem o contrato publicado pelo `ms-ofisy-billing` ([MESSAGING.md do billing](https://github.com/15SOAT-FIAP/ms-ofisy-billing/blob/master/docs/MESSAGING.md#eventos-publicados)): quoteId/totalPrice no `QuoteSentForApproval`, paymentId/checkoutUrl/amount no `PaymentCheckoutCreated`, paymentId/amount no `PaymentConfirmed` e paymentId/reason no `PaymentFailed`. O serviceOrderId não é duplicado no payload. O `checkoutUrl` é gravado junto com o aviso de checkout, para o cliente chegar ao link de pagamento.

| Evento | Tipo persistido | Texto |
|---|---|---|
| `QuoteSentForApproval` | `QUOTE_GENERATED` | “Orçamento #{quoteId} disponível para aprovação na ordem de serviço '{serviceOrderId}'. Valor total: R$ {valor}” |
| `PaymentCheckoutCreated` | `PAYMENT_CHECKOUT_CREATED` | “Pagamento disponível para a ordem de serviço '{serviceOrderId}'. Valor: R$ {valor}” |
| `PaymentConfirmed` | `PAYMENT_CONFIRMED` | “Pagamento confirmado para a ordem de serviço '{serviceOrderId}'. Valor: R$ {valor}” |
| `PaymentFailed` | `PAYMENT_FAILED` | “Pagamento recusado (ou expirado) para a ordem de serviço '{serviceOrderId}'.” |

Os avisos de orçamento e de pagamento são do cliente dono da OS, que o serviço descobre consultando o core (`GET /api/v1/service-orders/{id}/customer`). Core e Notification são consumidores independentes: o recebimento do evento não garante que o Core já tenha atualizado o status da OS, e a notificação não espera essa atualização.

## Testes automatizados do consumidor SQS

`NotificationEventListenerIT` usa JUnit, Spring Boot, Testcontainers e Awaitility. O teste sobe seu próprio LocalStack 4.9.0 com SNS/SQS/DynamoDB em porta dinâmica, inicializa os recursos com o mesmo script do Compose e inicia o listener real. A consulta do titular no Core é simulada no teste.

Os testes enviam mensagens e verificam a ação persistida: mensagem/referência/tipo de QuoteSentForApproval e StockLow; descarte de QuoteCreated e compensações; assinatura SNS por corpo sem atributos redundantes; redelivery após marcar leitura; retry, DLQ, progresso de outro grupo e reparo/replay de payloads inválidos. Para negativos e duplicatas, uma mensagem de controle no mesmo grupo FIFO confirma que a entrega anterior foi consumida; asserções não dependem de um sleep fixo.

`mvn test` executa os testes da fase de unidade. `mvn verify` também executa o consumidor pelo Maven Failsafe; Docker é obrigatório para essa etapa, e a classe não é silenciosamente pulada sem Docker. Para rodar somente a classe de mensageria:

```sh
mvn test-compile failsafe:integration-test failsafe:verify -Dit.test=NotificationEventListenerIT
```

Para validar a suíte inteira sem skips, mantenha DYNAMODB_TEST_ENDPOINT configurado para as integrações DynamoDB anteriores e rode `mvn clean verify`, seguido de `python dev/integration/assert_reports.py`. O verificador exige também NotificationEventListenerIT nos relatórios de Failsafe. O CI executa essa sequência e publica os relatórios de Surefire e Failsafe.

O teste MVC confirma que o titular pode listar, consultar e marcar o aviso como lido,
enquanto outro cliente recebe listas sem o aviso e `404` nas operações por ID.
