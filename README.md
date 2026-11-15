# ms-ofisy-notification

Microsserviço de notificações do Ofisy. Consome eventos de estoque e de orçamento/pagamento publicados via SNS/SQS, grava as notificações no DynamoDB e expõe uma API REST paginada para funcionários e clientes.

## Sobre o serviço

- Recebe eventos de `StockLow` (estoque baixo) e de cobrança (`QuoteSentForApproval`, `PaymentCheckoutCreated`, `PaymentConfirmed`, `PaymentFailed`) pela fila `ofisy-notification.fifo`.
- Cria cada notificação de forma idempotente: um mesmo `eventId` nunca gera duas notificações, e uma redelivery não altera o estado de leitura.
- Expõe consultas paginadas por cursor e marcação de leitura, separadas entre estoque e ordens de serviço.
- Autentica funcionários com JWT emitido pelo core. As rotas de notificação de ordem de serviço do cliente são protegidas pelo API Gateway e seu Lambda Authorizer.

O contexto geral do sistema está em [techchallenge-ofisy](https://github.com/15SOAT-FIAP/techchallenge-ofisy). As decisões deste serviço estão em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Stack

- Java 21 e Spring Boot 4.0.5 (Maven)
- Spring Cloud AWS 4.2.0 (SQS)
- AWS SDK v2 (DynamoDB Enhanced Client)
- Spring Security com JWT (jjwt)
- springdoc-openapi (Swagger UI)
- Spring Actuator e agente Datadog na imagem
- Testes: JUnit, Testcontainers (LocalStack), Awaitility, Maven Failsafe, JaCoCo

## Estrutura do projeto

O código segue Clean Architecture, com um pacote por caso de uso:

```
src/main/java/br/com/ofisy/notifications
├── domain/notification          # entidade, value objects, porta do repositório (sem Spring nem SDK AWS)
├── application/notification     # um UseCase e um Service por operação
├── adapters/
│   ├── controllers/notification # API REST (implementa NotificationApi)
│   ├── gateways/notification    # persistência DynamoDB e mapeamento
│   ├── listeners/notification   # consumidores SQS e handlers por tipo de evento
│   └── presenters/notification  # conversão para as respostas da API
├── config                       # DynamoDB, segurança, Swagger e tratamento de erros
└── shared                       # filtro JWT e rastreamento distribuído
```

Outros diretórios relevantes:

- `dev/integration/`: ambiente local com LocalStack, Docker Compose e cenários de mensageria.
- `k8s/`: manifestos de deploy no EKS (deployment, service, HPA, configmap e secret).
- `docs/`: arquitetura do serviço.
- `.github/workflows/`: CI, deploy (`cd.yml`) e remoção do ambiente (`destroy-app.yml`).

## Pré-requisitos

- JDK 21 e Maven (ou o wrapper `./mvnw`)
- Docker e Docker Compose, para o ambiente local e os testes de integração
- Make (opcional, mas recomendado para os comandos abaixo)

## Como executar

### Ambiente local completo (recomendado)

Sobe o LocalStack, cria os recursos AWS (tabela, filas e assinaturas) e a aplicação com o perfil `dev`:

```bash
make dev-up
```

Sem Make:

```bash
docker compose -f dev/integration/compose.yml up -d --build app
```

A API fica disponível em `http://localhost:18081/api/v1/notifications`. Para encerrar:

```bash
make dev-down
```

Não há volume persistente: o ambiente é descartável.

### Execução pela IDE ou linha de comando

Com o LocalStack já em execução (`docker compose -f dev/integration/compose.yml up -d localstack`), ative o perfil `dev`:

```bash
SPRING_PROFILES_ACTIVE=dev JWT_SECRET=<segredo-de-no-minimo-32-bytes> ./mvnw spring-boot:run
```

O perfil `dev` usa as credenciais fictícias `test/test` e o endpoint `http://localhost:14566`. Ele não deve ser usado em ambientes compartilhados.

### Cenários de mensageria

```bash
make dev-stock            # publica StockLow
make dev-quote            # publica QuoteSentForApproval (exige SERVICE_ORDER_ID=<UUID>)
make dev-quote-created    # QuoteCreated, que não deve criar notificação
make dev-stock-failure    # evento que não deve criar aviso de orçamento
make dev-invalid          # payload inválido, que vai para a DLQ
make dev-status           # estado das filas
make dev-replay           # reenvia a mensagem da DLQ após o reparo
```

O detalhamento de cada cenário está em [`dev/integration/README.md`](dev/integration/README.md).

## Configuração

Variáveis de ambiente:

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `JWT_SECRET` | sim, fora do perfil `dev` | — | Chave HMAC compartilhada com o emissor de tokens de funcionário. Mínimo de 32 bytes. Sem chave padrão fora do `dev`: a aplicação não sobe se a chave estiver ausente ou curta. |
| `APP_JWT_ISSUER` | não | `techchallenge-ofisy-api` | Issuer esperado nos tokens de funcionário. |
| `SPRING_CLOUD_AWS_REGION_STATIC` | não | `us-east-1` | Região AWS. |
| `SPRING_CLOUD_AWS_DYNAMODB_ENDPOINT` | não | endpoint AWS | Use para apontar para o LocalStack em containers. |
| `APP_CORE_BASE_URL` | só para avisos de orçamento no Compose | — | URL de um core acessível ao container. |
| `APP_CORE_SERVICE_TOKEN` | só para avisos de orçamento no Compose | — | Token de serviço usado para consultar o titular da ordem de serviço no core. |

Em produção, as credenciais AWS vêm da cadeia padrão do SDK (IAM roles). Não há credenciais de teste fora do perfil `dev`. O valor real de `JWT_SECRET` nunca deve ser versionado.

Propriedades principais (em `application.yml`):

- `app.aws.sqs.notification-queue`: fila de consumo (`ofisy-notification.fifo`).
- `app.aws.dynamodb.table-name`: tabela de notificações (`Notifications`).

## API

Base: `/api/v1/notifications`. A documentação interativa fica no Swagger UI (`/swagger-ui.html`) e no contrato OpenAPI (`/v3/api-docs`).

| Método | Caminho | Descrição | Autenticação |
|---|---|---|---|
| GET | `/stock` | Lista notificações de estoque | JWT de funcionário (ADMIN ou STOCKMAN) |
| GET | `/stock/unread` | Lista notificações de estoque não lidas | JWT de funcionário (ADMIN ou STOCKMAN) |
| GET | `/stock/{id}` | Consulta uma notificação de estoque | JWT de funcionário (ADMIN ou STOCKMAN) |
| PATCH | `/stock/{id}/read` | Marca uma notificação de estoque como lida | JWT de funcionário (ADMIN ou STOCKMAN) |
| GET | `/service-orders` | Lista notificações de ordens de serviço | JWT de funcionário ou cliente via Gateway |
| GET | `/service-orders/unread` | Lista notificações de ordens de serviço não lidas | JWT de funcionário ou cliente via Gateway |
| GET | `/service-orders/{id}` | Consulta uma notificação de ordem de serviço | JWT de funcionário ou cliente via Gateway |
| PATCH | `/service-orders/{id}/read` | Marca uma notificação de ordem de serviço como lida | JWT de funcionário ou cliente via Gateway |

Regras das listagens:

- `limit` tem padrão 50 e aceita valores de 1 a 100. Valor inválido retorna HTTP 400 em formato `ProblemDetail`.
- `nextToken` é opaco e específico da consulta. Token antigo, malformado ou de outra consulta retorna 400.
- Páginas vazias podem ter `nextToken` em consultas filtradas. Percorra até o `nextToken` vir `null`.

Rotas de ordem de serviço para cliente: o `X-Customer-Id` é obrigatório e é enviado pelo API Gateway depois de validar o JWT de cliente. O serviço não valida esse token localmente.

## Eventos consumidos

Os eventos chegam por tópicos SNS FIFO com filtro por `type` no corpo. A assinatura é feita pelo repositório de infraestrutura.

| Tópico SNS FIFO | Evento | Efeito |
|---|---|---|
| `ofisy-stock-events.fifo` | `StockLow` | Cria aviso de estoque baixo (`LOW_STOCK`). |
| `ofisy-billing-events.fifo` | `QuoteSentForApproval` | Cria aviso de orçamento disponível para aprovação (`QUOTE_GENERATED`). |
| `ofisy-billing-events.fifo` | `PaymentCheckoutCreated` | Cria aviso de checkout de pagamento criado. |
| `ofisy-billing-events.fifo` | `PaymentConfirmed` | Cria aviso de pagamento confirmado. |
| `ofisy-billing-events.fifo` | `PaymentFailed` | Cria aviso de falha no pagamento. |

Regras de entrega:

- Cada `eventId` é processado uma única vez (inserção condicional no DynamoDB).
- Falhas de infraestrutura são propagadas para retry. Só conflito de chave é tratado como evento já processado.
- Mensagens inválidas vão para a DLQ `ofisy-notification-dlq.fifo`.
- `QuoteCreated` não gera notificação. A notificação de orçamento nasce somente de `QuoteSentForApproval`.

Os contratos completos estão em [`dev/integration/README.md`](dev/integration/README.md).

## Testes

```bash
# testes unitários
./mvnw test

# testes unitários e de integração (exige Docker para o Testcontainers)
./mvnw clean verify

# teste concorrente de deduplicação contra DynamoDB Local
DYNAMODB_TEST_ENDPOINT=http://localhost:8000 ./mvnw clean verify

# somente o consumidor SQS, com LocalStack via Testcontainers
./mvnw test-compile failsafe:integration-test failsafe:verify -Dit.test=NotificationEventListenerIT

# suíte completa no ambiente local, com verificação dos relatórios
make dev-test
```

Sem `DYNAMODB_TEST_ENDPOINT`, o teste de concorrência é pulado. `mvn verify` executa o consumidor pelo Failsafe e exige Docker: a classe não é pulada em silêncio.

## Segurança

- O segredo JWT e as credenciais AWS reais nunca são versionados.
- O perfil `dev` e seus valores fictícios valem somente para LocalStack.
- A rota de cliente deve ser exposta apenas pela fronteira autenticada do API Gateway. A aplicação não deve oferecer uma entrada pública alternativa.
- Os caminhos de estoque continuam exigindo JWT de funcionário com perfil ADMIN ou STOCKMAN.

## Deploy

- `.github/workflows/cd.yml`: deploy no EKS, com manifestos em `k8s/`.
- `.github/workflows/destroy-app.yml`: remoção do ambiente.
- Recursos AWS (tabela, filas, tópicos, assinaturas e IAM) são provisionados pelo repositório de infraestrutura, não por este serviço.

## Documentação adicional

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): arquitetura e adaptações do microsserviço.
- [`dev/integration/README.md`](dev/integration/README.md): ambiente local, cenários de mensageria e contratos de eventos.
- [techchallenge-ofisy](https://github.com/15SOAT-FIAP/techchallenge-ofisy): core, com os ADRs e RFCs do sistema.

## Repositórios relacionados

- [techchallenge-ofisy](https://github.com/15SOAT-FIAP/techchallenge-ofisy): core (pedidos, estoque e faturamento).
- [techchallenge-ofisy-auth](https://github.com/15SOAT-FIAP/techchallenge-ofisy-auth): autenticação de clientes e Lambda Authorizer.
- [techchallenge-ofisy-eks-infra](https://github.com/15SOAT-FIAP/techchallenge-ofisy-eks-infra): infraestrutura EKS, API Gateway e observabilidade.
- [techchallenge-ofisy-rds-infra](https://github.com/15SOAT-FIAP/techchallenge-ofisy-rds-infra): infraestrutura do banco PostgreSQL.
