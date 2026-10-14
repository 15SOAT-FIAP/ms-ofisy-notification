# techchallenge-ofisy-notifications

## DynamoDB

Sem o perfil `local`, o cliente usa o endpoint AWS da região configurada em
`SPRING_CLOUD_AWS_REGION_STATIC` (padrão `us-east-1`) e a cadeia padrão de credenciais
do AWS SDK, incluindo IAM roles. Não há credenciais de teste nesse modo.

Para LocalStack, ative `SPRING_PROFILES_ACTIVE=local`. Esse perfil usa credenciais
fictícias `test/test` e `http://localhost:4566`. Em containers, configure
`SPRING_CLOUD_AWS_DYNAMODB_ENDPOINT` com o endereço do LocalStack acessível pelo container.
Tabela e índices devem ser provisionados antes do uso.
