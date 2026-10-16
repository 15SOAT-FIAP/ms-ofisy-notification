# Integração local e regressão do core

O Compose sobe a aplicação, DynamoDB Local e ElasticMQ (API compatível com SQS).
Não usa AWS real nem depende de credenciais de produção. Configure uma chave local
aleatória em `APP_JWT_SECRET` pelo ambiente ou por um arquivo externo ao Git.

Na raiz do repositório:

```sh
docker compose -f dev/integration/compose.yml up -d --build app
docker compose -f dev/integration/compose.yml run --rm --build --no-deps smoke
```

Para passar um arquivo de variáveis, acrescente `--env-file CAMINHO` antes de `-f`.
O smoke deve finalizar com `status: PASS`. Ele publica eventos reais na fila local,
consulta HTTP real e valida persistência, mensagens e campos do core, JWT, ausência,
tipo errado com 404, PATCH sem alteração indevida, deduplicação e listagens de não lidas.

API: `http://localhost:18081/api/v1/notifications`. DynamoDB: porta 18000.
SQS: porta 19324. As portas são publicadas somente em loopback.
O ambiente permanece rodando para exploração; para encerrá-lo:

```sh
docker compose -f dev/integration/compose.yml down
```

O DynamoDB deste ambiente é em memória; encerrá-lo descarta os dados locais de teste.
O bootstrap cria a tabela `Notifications`, os índices e a fila FIFO.

Os testes Java usam um endpoint isolado em `DYNAMODB_TEST_ENDPOINT`:

```sh
DYNAMODB_TEST_ENDPOINT=http://localhost:18000 mvn clean verify
```

O endpoint é apenas para testes: as tabelas temporárias são criadas e removidas pelos testes.
Não aponte essa variável para um ambiente de produção.

O smoke também percorre todas as páginas das quatro listagens HTTP com `limit=1`,
validando os tokens reais, ausência de itens perdidos/duplicados e o fim da paginação.
A suíte Java inclui `DynamoDbPaginationIntegrationTest` para páginas filtradas vazias com continuação.
