# Caso de uso: adicionar item

## Contexto

O serviço já cria pedidos por HTTP e persiste o pedido e seus itens no
PostgreSQL. A próxima evolução deve permitir acrescentar um item a um pedido
existente, preservando o UUID e recalculando o total a partir dos itens.

Esta etapa atravessa a aplicação, o port `Pedidos`, o domínio, a persistência
e o adapter REST. O caso de uso de criação permanece inalterado.

## Tarefa

Oferecer a operação de adicionar item a um pedido existente.

O comportamento esperado é:

- receber o UUID do pedido, o SKU, a quantidade e o preço unitário;
- buscar o pedido pelo port de saída `Pedidos`;
- recusar com `PedidoNaoEncontradoException` quando o UUID não existir;
- adicionar o item usando a regra do domínio;
- recusar quantidade ou preço não positivos com `ItemInvalidoException`;
- recusar pedido `PAGO` ou `CANCELADO` como pedido fechado;
- salvar o novo pedido pelo port `Pedidos`;
- devolver o pedido salvo;
- preservar o mesmo UUID e derivar o novo total pelos itens.

O endpoint será:

```text
POST /pedidos/{id}/itens
```

Requisição:

```json
{
  "sku": "CAFE-500",
  "quantidade": 1,
  "precoUnitario": 18.90
}
```

Resposta de sucesso: HTTP `200 OK`, com o pedido completo no formato de
`PedidoResponse`:

```json
{
  "id": "<mesmo-uuid-do-pedido>",
  "clienteId": "c-1",
  "itens": [
    {"sku": "CAFE-500", "quantidade": 2, "precoUnitario": 18.90},
    {"sku": "CAFE-500", "quantidade": 1, "precoUnitario": 18.90}
  ],
  "status": "ABERTO",
  "total": 56.70
}
```

## Regras

### Regras de negócio e HTTP

1. O pedido é buscado pelo port `Pedidos`, nunca diretamente pelo controller ou
   por uma entidade JPA.
2. UUID inexistente gera `PedidoNaoEncontradoException` e HTTP `404`.
3. Pedido `PAGO` ou `CANCELADO` é fechado e não aceita item; a recusa gera
   HTTP `409 Conflict`.
4. Quantidade zero ou negativa é inválida e gera HTTP `422`.
5. Preço unitário zero ou negativo também é inválido e gera HTTP `422`.
6. O item só é salvo depois que o domínio aceitar a operação.
7. Uma falha de busca, estado ou validação não grava pedido nem item.
8. O pedido salvo mantém o mesmo UUID do pedido buscado.
9. O total continua derivado: para `37.80` mais `1 × 18.90`, o resultado é
   `56.70`.
10. O endpoint de adicionar item responde `200 OK` em caso de sucesso.
11. O JSON usa `BigDecimal` no preço e números decimais para preços e total.

### Limites técnicos e de escopo

- Criar um port de entrada e um serviço de aplicação para adicionar item.
- Acrescentar busca por UUID ao port de saída `Pedidos` sem remover o método de
  salvar existente.
- Criar `PedidoNaoEncontradoException` sem dependência de framework.
- Ampliar o `PedidoExceptionHandler` existente para `404` e `409`, preservando
  os tratamentos atuais de `400` e `422`.
- Reutilizar `PedidoResponse` quando o formato de resposta for suficiente.
- O controller continua dependendo somente de ports de entrada.
- Não alterar `CriarPedido` nem o fluxo de criação.
- Não importar Spring, JPA, HTTP ou adapters em `domain/` ou `application/`,
  exceto frameworks nos adapters e na configuração já existente.
- Não alterar `pom.xml`, Docker, `application.yaml`, entidades JPA ou mapper
  nesta etapa.
- Não criar endpoints adicionais.
- Não usar Lombok, MapStruct, setters, `double` ou `float` para dinheiro.
- Não fazer commit.

## Casos de teste

### Aplicação sem Spring

| Caso | Dado | Quando | Então |
|---|---|---|---|
| Busca pedido existente | Port retorna um pedido `ABERTO` | Executar adicionar item | O pedido é encontrado e o domínio recebe o item |
| Pedido inexistente | Port não retorna UUID | Executar adicionar item | Gera `PedidoNaoEncontradoException` e não chama salvar |
| Pedido aberto aceita item | `37.80`, item `1 × 18.90` | Adicionar item | Retorna novo pedido com total `56.70` |
| UUID é preservado | Pedido existente com UUID conhecido | Adicionar item | O resultado mantém o mesmo UUID |
| Pedido fechado recusa | Pedido `PAGO` | Adicionar item | A operação falha e não chama salvar |
| Pedido cancelado recusa | Pedido `CANCELADO` | Adicionar item | A operação falha e não chama salvar |
| Quantidade zero recusa | Quantidade `0` | Adicionar item | Gera `ItemInvalidoException` e não chama salvar |
| Quantidade negativa recusa | Quantidade menor que zero | Adicionar item | Gera `ItemInvalidoException` e não chama salvar |
| Preço zero recusa | Preço `0` | Adicionar item | Gera `ItemInvalidoException` e não chama salvar |
| Preço negativo recusa | Preço menor que zero | Adicionar item | Gera `ItemInvalidoException` e não chama salvar |
| Pedido salvo | Pedido aberto e item válido | Executar adicionar item | O serviço chama `Pedidos.salvar` uma vez e devolve o pedido salvo |

### HTTP com PostgreSQL

| Caso | Entrada | Status | Efeito no banco |
|---|---|---:|---|
| Adição válida | UUID existente, `CAFE-500`, `1`, `18.90` | `200` | Uma linha de item é adicionada e total fica `56.70` |
| UUID inexistente | UUID ausente | `404` | Nenhuma linha é adicionada |
| Pedido pago | UUID de pedido `PAGO` | `409` | Nenhuma linha é adicionada |
| Pedido cancelado | UUID de pedido `CANCELADO` | `409` | Nenhuma linha é adicionada |
| Quantidade zero | Quantidade `0` | `422` | Nenhuma linha é adicionada |
| Quantidade negativa | Quantidade menor que zero | `422` | Nenhuma linha é adicionada |
| Preço inválido | Preço zero ou negativo | `422` | Nenhuma linha é adicionada |
| JSON malformado | Corpo inválido | `400` | Nenhuma linha é adicionada |

## Definição de pronto

- Existe um caso de uso de adicionar item que busca pelo port `Pedidos`.
- UUID inexistente resulta em `PedidoNaoEncontradoException` na aplicação e
  `404` no HTTP.
- Pedidos `PAGO` e `CANCELADO` resultam em `409` e não são salvos.
- Quantidade e preço inválidos resultam em `422` e não são salvos.
- O caso válido `37.80 + 1 × 18.90` resulta em `56.70`.
- O UUID do pedido permanece igual antes e depois da adição.
- O item aceito é salvo pelo port `Pedidos`.
- `POST /pedidos/{id}/itens` responde `200 OK` com o pedido completo.
- O fluxo de `CriarPedido` permanece inalterado.
- Testes unitários executam sem Spring ou Postgres.
- O cenário HTTP é executado explicitamente contra o PostgreSQL pelo Maven
  Wrapper.
- As recusas comprovam que nenhuma linha foi adicionada.
- Não há imports de JPA ou Spring em `application/` ou `domain/`.
- Nenhum arquivo fora dos limites aprovados foi alterado e nenhum commit foi
  criado.

## Limites de arquivos para a implementação posterior

Produção permitida:

- criar `application/port/in/AdicionarItem.java`;
- criar `application/service/AdicionarItemService.java`;
- alterar `application/port/out/Pedidos.java` para busca por UUID;
- criar `application/exception/PedidoNaoEncontradoException.java`;
- alterar `adapter/entrada/rest/PedidoController.java`;
- alterar `adapter/entrada/rest/PedidoExceptionHandler.java`;
- criar um request REST específico para o item, se necessário;
- alterar `config/` somente se a composição do novo caso de uso exigir um bean.

Testes permitidos:

- criar teste unitário do serviço de adicionar item;
- criar ou alterar teste de integração do controller REST;
- alterar teste de persistência somente se a busca pelo port exigir cobertura
  adicional.

Fora do escopo: `CriarPedido`, classes do domínio, `pom.xml`,
`application.yaml`, Docker, entidades JPA, mapper JPA, novos endpoints e
qualquer commit.
