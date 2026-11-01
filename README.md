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

Defina `APP_JWT_SECRET` externamente com a mesma chave HMAC usada pelo emissor,
com pelo menos 32 bytes (e tamanho compatível com o algoritmo de assinatura).
Fora do perfil `dev`, a aplicação não possui chave padrão e falha na inicialização se a chave estiver
ausente, vazia ou for curta demais. Nunca versione o valor real.

Defina `APP_JWT_ISSUER` com o issuer exato do token emitido pelo serviço de autenticação
(padrão `techchallenge-ofisy-api`). O filtro valida esse valor configurado.
Se a antiga chave numérica tiver sido usada em algum ambiente acessível, ela deve
ser substituída no emissor e no consumidor, invalidando os tokens antigos.

## Eventos consumidos e ambiente local

- StockLow: aviso de estoque baixo.
- QuoteSentForApproval: orçamento disponível para aprovação após a etapa de estoque; serviceOrderId no envelope, quoteId/totalPrice no payload.

QuoteCreated não cria notificação de orçamento. A criação de orçamento ocorre pela entrada idempotente de evento; execute(command) de orçamento e a mensagem legada de orçamento gerado foram descomissionados. O tipo interno QUOTE_GENERATED permanece igual. Para subir LocalStack, publicar cenários e executar a integração, consulte [dev/integration/README.md](dev/integration/README.md). O filtro da assinatura SNS na AWS deve ser alinhado na infraestrutura responsável; o bootstrap local não altera a conta AWS.
