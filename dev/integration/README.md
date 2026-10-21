# Integração local e regressão do core

O Compose sobe aplicação e LocalStack 4.9.0 com SNS, SQS e DynamoDB.
Usa somente credenciais fictícias locais. Configure uma chave aleatória de pelo menos
32 bytes em APP_JWT_SECRET no ambiente, sem versioná-la.

Na raiz do repositório:

~~~sh
docker compose -f dev/integration/compose.yml up -d --build app
docker compose -f dev/integration/compose.yml run --rm --build --no-deps smoke
~~~

O bootstrap provisiona uma tabela Notifications com os índices existentes, dois tópicos
SNS FIFO de produtores simulados (ofisy-stock.fifo e ofisy-billing.fifo), assinaturas
com filtro pelo atributo type e raw delivery, uma fila ofisy-notification.fifo e sua
DLQ FIFO. São recursos exclusivamente locais; este Compose não substitui o Terraform
da infraestrutura. O limite local de três recebimentos e visibility timeout de três
segundos agilizam a validação de retry/DLQ.

O envelope JSON tem eventId (UUID), type, serviceOrderId (UUID quando aplicável),
occurredAt (Instant) e payload (objeto). Os tipos atuais permanecem LOW_STOCK e
QUOTE_GENERATED. Estoque usa stockId como grupo quando não há OS; orçamento usa o ID
da OS, igual no envelope e no payload. Não há compatibilidade com eventType.
Em produção, o produtor usa eventId como deduplicação; o smoke usa dedup IDs distintos
para forçar reentrega e exercitar a idempotência persistente.

O smoke deve terminar com status PASS. Publica pelo SNS, percorre a aplicação e o
DynamoDB e consulta HTTP real. Valida campos/mensagens do core, JWT, 404 por ausência e
tipo errado, PATCH sem alteração indevida, duplicatas preservando leitura e datas e
paginação das quatro rotas com limit=1. Também verifica filtro SNS, payload inválido
indo à DLQ após retries e processamento de outro grupo durante a falha.

Para reprocessamento manual, o smoke recebe a mensagem na DLQ, corrige o payload,
republica mantendo eventId com nova chave de deduplicação, confirma a persistência e
somente então remove a mensagem da DLQ. O procedimento não elimina mensagens sem
confirmar o processamento.

API: http://localhost:18081/api/v1/notifications
LocalStack (SNS, SQS, DynamoDB): http://localhost:14566
As portas são publicadas somente em loopback.

Os testes Java usam endpoint isolado em DYNAMODB_TEST_ENDPOINT:

~~~sh
DYNAMODB_TEST_ENDPOINT=http://localhost:14566 mvn clean verify
~~~

As tabelas temporárias são criadas e removidas pelos testes. O endpoint deve ser local.
O ambiente fica disponível para exploração. Para encerrá-lo:

~~~sh
docker compose -f dev/integration/compose.yml down
~~~

Os dados deste ambiente são temporários e descartados ao remover os containers.