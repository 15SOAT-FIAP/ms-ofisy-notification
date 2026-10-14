# techchallenge-ofisy-notifications

## DynamoDB

Sem o perfil `local`, o cliente usa o endpoint AWS da região configurada em
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
