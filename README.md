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

As rotas `/api/v1/notifications/service-orders` e seus subcaminhos preservam o contrato
público do core no serviço. A autenticação do cliente é responsabilidade do API Gateway
e de seu Lambda Authorizer (ADR-0006/ADR-0010). O token da Lambda tem issuer
`techchallenge-ofisy-auth`, subject com o customerId e não contém perfil de funcionário;
por isso, o filtro de funcionário não o interpreta nessas rotas. Um mesmo matcher é
usado na autorização HTTP e na exclusão do filtro, respeitando os limites do caminho
e o context path. Caminhos como `service-orders-admin` não são incluídos.

O serviço não passa a validar localmente o JWT de cliente nem a extrair permissões de
funcionário dele. No deploy, essas rotas devem ser expostas pela fronteira autenticada
do Gateway; a aplicação não deve ganhar uma entrada pública alternativa que contorne
o authorizer. As rotas de estoque exigem JWT de funcionário com perfil ADMIN ou STOCKMAN.

`JwtCustomerRoutesIntegrationTest` exercita a cadeia real de segurança com MockMvc e
endpoints de teste. Cobre o formato HS256
da Lambda com chave distinta da de funcionário, GET/PATCH públicos, context path,
query string, caminhos parecidos e a manutenção da proteção de estoque. O teste não
substitui uma validação do Gateway implantado na AWS.

## Eventos consumidos e ambiente local

- StockLow: aviso de estoque baixo.
- QuoteSentForApproval: orçamento disponível para aprovação após a etapa de estoque; serviceOrderId no envelope, quoteId/totalPrice no payload.

QuoteCreated não cria notificação de orçamento. A criação de orçamento ocorre pela entrada idempotente de evento; execute(command) de orçamento e a mensagem legada de orçamento gerado foram descomissionados. O tipo interno QUOTE_GENERATED permanece igual. Para subir LocalStack, publicar cenários e executar a integração, consulte [dev/integration/README.md](dev/integration/README.md). O filtro da assinatura SNS na AWS deve ser alinhado na infraestrutura responsável; o bootstrap local não altera a conta AWS.

O teste [NotificationEventListenerIT](src/test/java/br/com/ofisy/notifications/adapters/listeners/notification/NotificationEventListenerIT.java) sobe SNS/SQS/DynamoDB automaticamente com Testcontainers, publica mensagens e verifica os efeitos do listener real. Ele roda em `mvn verify` pelo Failsafe e exige Docker, sem depender de uma fila AWS ou do ambiente Compose já aberto. A documentação de integração explica os cenários e como executar a suíte inteira sem skips.

## OpenAPI

Com o ambiente local iniciado, acesse o Swagger UI em
`http://localhost:18081/swagger-ui.html` e a especificação em
`http://localhost:18081/v3/api-docs`. No Swagger UI, use **Authorize** para
informar um JWT de funcionário com perfil ADMIN ou STOCKMAN e testar as rotas
de estoque. As rotas de ordens de serviço não exigem Bearer diretamente no
microsserviço. O Gateway aplica a autenticação de cliente quando publicado.
