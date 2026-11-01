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

Consultas e marcação de leitura usam os casos de uso específicos de estoque e ordem de serviço do core.
ID ausente e ID de outro tipo geram a mesma `NotificationNotFoundException`, sem alterar leitura ou datas.
As rotas por id são `/stock/{id}` e `/service-orders/{id}`, com os respectivos PATCHs `/read`.
Tipo, referências e mensagem são definidos pelas fábricas e value objects do domínio, orquestrados pelos casos de criação.
O listener traduz o envelope SQS para commands e não constrói entidades nem escolhe `NotificationType`.

A criação de estoque preserva `execute(command)` do core. Para orçamento, essa entrada legada foi descomissionada: a interface oferece somente `executeEvent(eventId, command)`. A criação por evento usa o identificador do evento para a inserção condicional; uma duplicata não atualiza um registro existente.
Mantemos a adaptação de listagens por cursor para DynamoDB, com `items` e `nextToken`, em vez de listas sem paginação.
A resposta restaura os campos `stockId` e `quoteId` do core; `referenceId` permanece como alias aditivo da migração.
DynamoDB substitui JPA conforme ADR-0013; portanto não copiamos entidades JPA, Flyway ou transações relacionais.
A criação por evento usa operação condicional; atualização de leitura é uma operação distinta.


## Paginação e categorias

A porta e os casos de listagem recebem um cursor opaco (`String`), e `PaginatedResult`
devolve `nextCursor`. Domínio e aplicação apenas repassam esse valor, sem interpretar
chaves, atributos, JSON ou Base64. O adapter de persistência interpreta o cursor e converte
as chaves DynamoDB. Na API, `items` e `nextToken` permanecem com o mesmo contrato.
As fábricas genéricas de criação e reconstrução tratam cada `NotificationType` com switch
expression exaustiva, sem fallback para orçamento, e rejeitam tipo nulo explicitamente.
As regras para os tipos válidos, referências, leitura e datas são preservadas. A mudança explícita da criação de orçamento por evento está descrita abaixo.

## Entrada de orçamento por evento

O consumidor de orçamento recebe QuoteSentForApproval, após a etapa de estoque prevista na coreografia de Billing. QuoteCreated não cria o aviso de aprovação em Notification. A mensagem de disponibilidade para aprovação é composta por NotificationMessage.forQuoteSentForApproval, chamada por executeEvent. A entrada execute(command) de orçamento e sua fábrica de mensagem de orçamento gerado foram removidas por decisão explícita de descomissionamento. O nome externo do evento não muda o enum interno/persistido QUOTE_GENERATED, o schema DynamoDB ou os contratos HTTP.
