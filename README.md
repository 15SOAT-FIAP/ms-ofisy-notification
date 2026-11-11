# techchallenge-ofisy-notifications

## DynamoDB

Fora dos perfis `local` e `dev`, o cliente usa o endpoint AWS da região configurada em
`SPRING_CLOUD_AWS_REGION_STATIC` (padrão `us-east-1`) e a cadeia padrão de credenciais
do AWS SDK, incluindo IAM roles. Não há credenciais de teste nesse modo.

Para LocalStack, ative `SPRING_PROFILES_ACTIVE=local`. Esse perfil usa credenciais
fictícias `test/test` e `http://localhost:4566`. Em containers, configure
`SPRING_CLOUD_AWS_DYNAMODB_ENDPOINT` com o endereço do LocalStack acessível pelo container.
Tabela e índices devem ser provisionados antes do uso.

## Deduplicação de eventos

O consumidor usa inserção condicional (`attribute_not_exists(id)`) para que somente
uma entrega de cada `eventId` crie a notificação. Redelivery não altera uma notificação
existente, incluindo seu estado de leitura. Falhas de infraestrutura continuam sendo
propagadas para retry; somente conflito de chave é tratado como evento já processado.

Para executar o teste concorrente contra DynamoDB Local, defina
`DYNAMODB_TEST_ENDPOINT=http://localhost:8000` e execute `mvn clean verify`.
O teste cria e remove uma tabela temporária exclusiva; sem essa variável ele é pulado.

## JWT

Defina `JWT_SECRET` externamente com a mesma chave HMAC usada pelo emissor de funcionário,
com pelo menos 32 bytes (e tamanho compatível com o algoritmo de assinatura).
Fora do perfil `dev`, a aplicação não possui chave padrão e falha na inicialização se a chave estiver
ausente, vazia ou for curta demais. Nunca versione o valor real.

Defina `APP_JWT_ISSUER` com o issuer exato do token de funcionário emitido pelo core
(padrão `techchallenge-ofisy-api`). O filtro valida assinatura, emissor, expiração,
subject e a claim `role` singular antes de converter o perfil em uma autoridade Spring.

Nas quatro rotas de ordem de serviço, o Gateway sobrescreve `X-Customer-Id` com a
identidade autenticada pelo Lambda Authorizer. O Notification valida o JWT de cliente
emitido por `techchallenge-ofisy-auth` e confere se o subject coincide com o header.
Configure `APP_JWT_CUSTOMER_SECRET` se a Lambda usar uma chave distinta de `JWT_SECRET`.
As listagens e os cursores ficam restritos ao cliente. Consulta por ID e marcação de
leitura retornam `404` quando a notificação pertence a outra pessoa. As rotas de estoque
exigem JWT de funcionário com perfil ADMIN ou STOCKMAN.

Ao consumir `QuoteSentForApproval`, o serviço resolve o cliente da OS pelo Core.
Configure `APP_CORE_BASE_URL` e `APP_CORE_SERVICE_TOKEN` para usar a rota autenticada
`GET /api/v1/service-orders/{id}/customer`. O Compose independente usa uma fixture
de Core. Para registros anteriores, consulte o [guia de backfill](dev/integration/README.md#backfill-de-titularidade).

## Eventos consumidos e ambiente local

O Billing também publica `PaymentCheckoutCreated`, `PaymentConfirmed` e
`PaymentFailed`. O Notification grava cada aviso para o dono da ordem de serviço e
o disponibiliza pelas mesmas rotas autenticadas de notificações da OS. O checkout
expõe `paymentId` e `checkoutUrl`. A confirmação, a recusa e a expiração expõem
`paymentId` e uma mensagem de situação. Os campos de pagamento são omitidos nas
respostas de estoque e orçamento.

O webhook do Mercado Pago é recebido pelo Billing. O Notification recebe somente
os eventos publicados no SNS/SQS, com deduplicação por `eventId`.

- StockLow: aviso de estoque baixo.
- QuoteSentForApproval: orçamento disponível para aprovação após a etapa de estoque; serviceOrderId no envelope, quoteId/totalPrice no payload.

QuoteCreated não cria notificação de orçamento. A criação de orçamento ocorre pela entrada idempotente de evento; execute(command) de orçamento e a mensagem legada de orçamento gerado foram descomissionados. O tipo interno QUOTE_GENERATED permanece igual. Para subir LocalStack, publicar cenários e executar a integração, consulte [dev/integration/README.md](dev/integration/README.md). O filtro da assinatura SNS na AWS deve ser alinhado na infraestrutura responsável; o bootstrap local não altera a conta AWS.

O teste [NotificationEventListenerIT](src/test/java/br/com/ofisy/notifications/adapters/listeners/notification/NotificationEventListenerIT.java) sobe SNS/SQS/DynamoDB automaticamente com Testcontainers, publica mensagens e verifica os efeitos do listener real. Ele roda em `mvn verify` pelo Failsafe e exige Docker, sem depender de uma fila AWS ou do ambiente Compose já aberto. A documentação de integração explica os cenários e como executar a suíte inteira sem skips.

## OpenAPI

Com o ambiente local iniciado, acesse o Swagger UI em
`http://localhost:18081/swagger-ui.html` e a especificação em
`http://localhost:18081/v3/api-docs`. No Swagger UI, use **Authorize** para
informar um JWT de funcionário com perfil ADMIN ou STOCKMAN e testar as rotas
de estoque. Para ordens de serviço, faça a chamada pela integração do Gateway
com um JWT de cliente. O Gateway injeta o header de identidade.
