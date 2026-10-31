# Pagamentos assíncronos simulados

## Contexto

O pedido precisa ser pago de forma assíncrona por um processador simulado,
conectado pelo RabbitMQ já presente no Compose local. Não há cobrança real.

Postgres e RabbitMQ não participam de uma transação única. A consistência vem
de estado persistido antes da publicação, idempotência pela `solicitacaoId`,
confirmação do broker e republicação quando o cliente repete a mesma
`solicitacaoId`. Não há outbox nem reenvio automático neste laboratório.

Hoje o domínio possui `Pedido.pagar()`, que vai de `ABERTO` direto para `PAGO`.
Esse caminho síncrono sai da versão assíncrona.

## Mudança de contrato

| Antes | Agora |
|---|---|
| O domínio permitia pagar direto: `ABERTO` → `PAGO` em uma chamada, sem processador | `Pedido.pagar()` foi removido; `PAGO` só é alcançado por um resultado de pagamento vindo do broker |
| Não havia como pedir pagamento por HTTP | `POST /pedidos/{pedidoId}/pagamentos` com `solicitacaoId` e `cenario` |
| — | O cliente não informa valor nem aprovação; o valor vem de `pedido.total()` e enviar `valor` é `400` |
| — | `202 Accepted` significa solicitação registrada, confirmada e roteada; **nunca** pagamento aprovado |
| — | O resultado é acompanhado por `GET /pedidos/{id}`: `EM_PAGAMENTO`, depois `PAGO` ou de volta a `ABERTO` |

Não existia endpoint HTTP de aprovação enviada pelo cliente neste repositório;
o único atalho síncrono era `Pedido.pagar()` no domínio, removido na etapa 1.

## Tarefa

### HTTP

`POST /pedidos/{pedidoId}/pagamentos`

```json
{
  "solicitacaoId": "<uuid>",
  "cenario": "APROVAR"
}
```

- `cenario` aceita `APROVAR` ou `RECUSAR`.
- O valor é `pedido.total()`; o cliente não informa valor. Corpo com `valor`
  resulta em `400`.
- `202 Accepted` com `{"solicitacaoId", "pedidoId", "status": "EM_PAGAMENTO"}`
  significa apenas solicitação aceita, persistida, confirmada e roteada pelo
  broker. **Nunca significa aprovado.**

| Situação | Status |
|---|---:|
| Solicitação aceita e roteada | `202` |
| JSON malformado, UUID inválido, cenário desconhecido ou `valor` enviado | `400` |
| Pedido inexistente | `404` |
| Pedido fora de `ABERTO` | `409` |
| `solicitacaoId` reutilizada com outro pedido ou cenário | `409` |
| Broker não confirmou ou não roteou | `503` |

`GET /pedidos/{id}` devolve `200` com `PedidoResponse` ou `404`. É por ele que o
cliente acompanha o resultado. `POST /pedidos` permanece como está.

### Estados

```text
ABERTO --solicitar--> EM_PAGAMENTO --aprovado--> PAGO
                      EM_PAGAMENTO --recusado--> ABERTO
```

- Itens não mudam em `EM_PAGAMENTO`: adicionar item resulta em `409`.
- Cancelar em `EM_PAGAMENTO` é recusado nesta versão.
- `Pedido.pagar()` é substituído pelas transições acima.

### Topologia

Todos os exchanges e filas são duráveis e declarados na primeira subida.

| Exchange (direct) | Chave | Fila principal | Fila de erro |
|---|---|---|---|
| `pagamentos.comandos` | `pagamento.solicitar` | `pagamentos.solicitacoes` | `pagamentos.solicitacoes.erros` |
| `pagamentos.eventos` | `pagamento.resultado` | `pedidos.resultados` | `pedidos.resultados.erros` |

- As filas de erro são declaradas desde o início.
- As filas principais nascem com `x-dead-letter-exchange: ""` e
  `x-dead-letter-routing-key: <fila principal>.erros`.
- Argumentos de filas existentes não são alterados. Redeclarar com outros
  argumentos falha com `PRECONDITION_FAILED`; mudança futura deve usar policy.
- Mensagem rejeitada sem requeue vai para a fila de erro correspondente.

### Mensagens JSON versão 1

Persistentes (`delivery_mode=2`), `content_type=application/json`, valor como
número decimal (`BigDecimal`), nunca string.

Solicitação:

```json
{"versao": 1, "solicitacaoId": "<uuid>", "pedidoId": "<uuid>", "valor": 37.80, "cenario": "APROVAR"}
```

Resultado:

```json
{"versao": 1, "solicitacaoId": "<uuid>", "pedidoId": "<uuid>", "valor": 37.80, "aprovado": true, "motivo": "Aprovado pelo simulador"}
```

Versão diferente de `1` ou JSON inválido é rejeitado para a fila de erro.

### Simulador

Adapter no mesmo processo, em pacote próprio, sem acesso ao repositório nem ao
banco de pedidos. Consome `pagamentos.solicitacoes` e publica em
`pagamentos.eventos` com base apenas no `cenario`: `APROVAR` gera
`aprovado=true`; `RECUSAR` gera `aprovado=false` com motivo. É determinístico,
então reprocessar a mesma solicitação produz o mesmo resultado.

## Regras

### Publicação e `202`

1. Em uma transação no Postgres: o pedido passa a `EM_PAGAMENTO` e a solicitação
   (`solicitacaoId`, `pedidoId`, `valor`, `cenario`) é gravada como
   `PENDENTE_PUBLICACAO`.
2. Publicar com publisher confirms e `mandatory=true`.
3. Somente com `ack` e sem `basic.return` a solicitação vira `PUBLICADA` e a
   resposta é `202`.

### Repetição persistente

- Mesma `solicitacaoId` com mesmo pedido e cenário é idempotente: responde `202`
  sem criar outra solicitação; se ainda estiver `PENDENTE_PUBLICACAO`, republica.
- Mesma `solicitacaoId` com outro pedido ou cenário resulta em `409`.
- A chave fica no Postgres e sobrevive a reinícios.

### Falha de publicação

- `nack`, `basic.return`, timeout ou broker indisponível resultam em `503`.
- A solicitação permanece `PENDENTE_PUBLICACAO` e o pedido, `EM_PAGAMENTO`.
- Recuperação: somente quando o cliente repete com a mesma `solicitacaoId` e o
  mesmo conteúdo. Sem outbox e sem reenvio automático neste laboratório: uma
  pendente que o cliente não repete continua pendente, com o pedido em
  `EM_PAGAMENTO`.
- Publicação duplicada é segura porque o consumo do resultado é idempotente.

### Consumo do resultado

| Situação | Efeito | Mensagem |
|---|---|---|
| Primeiro resultado de solicitação em `EM_PAGAMENTO` | Aplica `PAGO` ou volta a `ABERTO` | `ack` |
| Resultado repetido igual ao já aplicado | Nenhum | `ack` |
| Resultado conflitante: outro `aprovado`, `valor` ou `pedidoId` diferente do registrado | Pedido não muda; registra log | rejeita para `pedidos.resultados.erros` |
| Solicitação desconhecida | Nenhum | rejeita para `pedidos.resultados.erros` |

### Reinício

- Filas duráveis e mensagens persistentes sobrevivem ao reinício do broker.
- Mensagens sem `ack` voltam para a fila e são reprocessadas de forma idempotente.
- Solicitações `PENDENTE_PUBLICACAO` sobrevivem ao reinício da aplicação no
  Postgres e aguardam a repetição do cliente; a aplicação não as reenvia sozinha.

### Limites

- Sem cobrança real, sem banco compartilhado entre serviços e sem afirmar
  transação única entre Postgres e RabbitMQ.
- Domínio e aplicação não importam Spring, JPA, AMQP ou HTTP; publicação e
  persistência da solicitação são portas de saída.
- Valores monetários usam `BigDecimal`.
- Sem Lombok ou MapStruct.
- `spring-boot-starter-amqp` exige autorização antes de entrar no `pom.xml`.
- Subir o serviço `postgres` do Compose para os `*IT` exige autorização.
- Não alterar argumentos de filas existentes nem remover containers ou volumes.

## Definição de pronto

- `StatusPedido` possui `EM_PAGAMENTO`; `Pedido.pagar()` não existe mais.
- Transições válidas e inválidas cobertas por testes de domínio, incluindo itens
  bloqueados em `EM_PAGAMENTO`.
- `GET /pedidos/{id}` responde `200` e `404`.
- O caso de uso de solicitar pagamento é testado sem Spring para `202`, `404`,
  `409` de estado, `409` de chave reutilizada, repetição idempotente e falha de
  publicação.
- A solicitação é persistida antes da publicação e sobrevive a reinício.
- `202` só ocorre após confirm e roteamento; mensagem não roteada resulta em
  `503`.
- As quatro filas e os dois exchanges existem; as filas principais têm os
  argumentos de dead-letter esperados.
- Aprovação leva a `PAGO`; recusa volta a `ABERTO`.
- Resultado duplicado não altera o pedido; resultado conflitante e JSON inválido
  vão para a fila de erro.
- Pendente é republicada quando o cliente repete a mesma chave e conteúdo.
- Testes unitários passam sem banco e sem broker; `*IT` rodam explicitamente
  contra Postgres e RabbitMQ locais pelo Maven Wrapper.

## Etapas de implementação

Cada etapa tem plano e OK próprios.

A primeira etapa do laboratório, "Pedidos recebe e publica a solicitação",
corresponde às etapas 0 a 5. O serviço de Pagamentos (etapa 6) é a etapa
seguinte. A etapa 7 está fora deste laboratório, que não tem reenvio
automático; ela foi implementada e depois revertida.

| # | Etapa | Verificação |
|---|---|---|
| 0 | Corrigir compilação de `PedidoControllerTest` | `mvnw test` verde |
| 1 | Domínio: `EM_PAGAMENTO` e transições; remover `pagar()` | `PedidoTest` |
| 2 | `GET /pedidos/{id}` | unitário e `PedidoControllerIT` |
| 3 | Caso de uso de solicitar pagamento com portas de saída | unitário com fakes |
| 4 | Persistência JPA da solicitação | `SolicitacaoPagamentoJpaAdapterIT` |
| 5 | Topologia AMQP, publicador com confirms e endpoint | `PagamentoPublicacaoIT` |
| 6 | Simulador e consumidor de resultados | ITs de aprovado, recusado, duplicado, conflitante e inválido |
| 7 | ~~Republicação de pendentes ao iniciar~~ (fora do laboratório: sem reenvio automático) | — |
