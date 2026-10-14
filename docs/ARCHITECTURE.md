# Arquitetura de notificações

O serviço segue as convenções de Clean Architecture e DDD documentadas no
[techchallenge-ofisy](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/README.md).

- `domain/notification`: entidade, value object, porta de repositório e exceções do agregado; sem Spring ou SDK AWS.
- `application/notification/<caso>`: uma interface `UseCase` e um `Service` por operação, com entrada pelo método `execute` e dependência da porta do domínio.
- `adapters/gateways/notification`: persistência DynamoDB e mapper de entidade separados.
- `adapters/controllers/notification`: controller implementa `NotificationApi`; DTOs ficam no subpacote `dto`.
- `adapters/presenters/notification`: converte o domínio em respostas da API.
- `adapters/listeners/notification`: recebe eventos SQS e chama a interface do caso de uso de criação.
- `config` e `shared`: integração com Spring/AWS e segurança transversal.

Os adapters são acrescentados nos PRs seguintes da stack. O domínio não conhece transporte HTTP, SQS ou DynamoDB.
As interfaces de aplicação não dependem de adapters; os services podem usar `@Service`, como no projeto de referência.

## Adaptações do microsserviço

Preservamos os contratos atuais: paginação por cursor, `referenceId` na resposta e rotas por id unificadas.
Esses contratos não são idênticos aos endpoints/listas do monólito e não foram alterados nesta refatoração.
DynamoDB substitui JPA conforme ADR-0013; portanto não copiamos entidades JPA, Flyway ou transações relacionais.
A criação por evento usa operação condicional; atualização de leitura é uma operação distinta.
