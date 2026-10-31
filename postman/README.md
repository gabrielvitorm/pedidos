# Collection da API de Pedidos

Arquivos para importar no Postman (ou em outra ferramenta compatível com Collection v2.1):

- `pedidos.postman_collection.json`: requests na ordem do roteiro, com a resposta esperada na descrição e testes de status.
- `local.postman_environment.json`: environment `Pedidos - local`.

A collection chama a API real. Não há endpoint de aprovação nem mock: nesta etapa do
laboratório não existe consumidor de Pagamentos, então o pedido fica `EM_PAGAMENTO` depois
da solicitação e a mensagem fica na fila `pagamentos.solicitacoes`. `202` nunca significa aprovado.

## Variáveis

| Variável | Valor | Origem |
|---|---|---|
| `base_url` | `http://localhost:8080` | fixa |
| `pedido_id` | vazio | copiar o `id` da resposta do request 01 |
| `pedido_recusa_id` | vazio | copiar o `id` da resposta do request 05 |
| `solicitacao_aprovar_id` | UUID fixo | estável: o mesmo valor em todo reenvio |
| `solicitacao_recusar_id` | UUID fixo | estável |
| `solicitacao_pedido_ausente_id` | UUID fixo | estável; nunca é gravada |
| `pedido_inexistente_id` | `00000000-0000-0000-0000-000000000000` | fixa |

Nenhuma variável gera UUID automaticamente. Não há credenciais: a API não tem autenticação.

## Antes de começar

1. Postgres e RabbitMQ do Compose no ar (`infra/docker-compose.yml`).
2. Aplicação rodando com JDK 21. Neste ambiente o Postgres do projeto está na porta 5433:

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
   $env:POSTGRES_PORT = "5433"
   .\mvnw.cmd spring-boot:run
   ```

3. No Postman: Import → selecione os dois arquivos da pasta `postman/` → escolha o
   environment `Pedidos - local` no seletor do canto superior direito.

## Roteiro manual

| # | Request | Esperado | Ação depois |
|---|---|---|---|
| 01 | Criar pedido (aprovação) | `201`, `ABERTO`, `total: 37.80` | Copie o `id` da resposta e cole em `pedido_id` (Environments → Pedidos - local → coluna Current value). Salve. |
| 02 | Consultar pedido | `200`, `ABERTO` | — |
| 03 | Solicitar pagamento - APROVAR | `202`, `EM_PAGAMENTO` | Opcional: no painel `http://127.0.0.1:15672`, fila `pagamentos.solicitacoes` → Get messages mostra a mensagem com `"valor":37.80`. |
| 04 | Consultar pedido novamente | `200`, `EM_PAGAMENTO` | — |
| 05 | Criar pedido (recusa) | `201`, `ABERTO` | Copie o `id` da resposta e cole em `pedido_recusa_id`. Salve. |
| 06 | Consultar pedido (recusa) | `200`, `ABERTO` | — |
| 07 | Solicitar pagamento - RECUSAR | `202`, `EM_PAGAMENTO` | — |
| 08 | Consultar pedido (recusa) novamente | `200`, `EM_PAGAMENTO` | — |
| 09 | Repetição - mesma chave e conteúdo | `202`, mesma `solicitacaoId` | A fila continua com uma única mensagem para essa chave. |
| 10 | Conflito - mesma chave, outro cenário | `409` com `mensagem` | — |
| 11 | Pedido ausente - solicitar pagamento | `404` com `mensagem` | — |
| 12 | Pedido ausente - consultar | `404` com `mensagem` | — |

Os requests 03, 07 e 09 podem ser reenviados quantas vezes quiser: a chave é a mesma e a
resposta continua `202`, sem nova mensagem.

## Repetir o roteiro do zero

As chaves de solicitação ficam gravadas no Postgres. Para refazer o roteiro com pedidos
novos no mesmo banco, troque **uma vez** os valores de `solicitacao_aprovar_id` e
`solicitacao_recusar_id` por outros UUIDs antes do request 01. Sem essa troca, o request 03
com um `pedido_id` novo responde `409`, porque a chave já pertence ao pedido anterior.

## Quando houver o consumidor de Pagamentos

Os requests 04 e 08 passarão a mostrar `PAGO` e `ABERTO` (com os mesmos itens),
respectivamente, depois que o resultado for processado.
