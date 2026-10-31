# Caso de uso: criar pedido

## Contexto

O domínio de pedidos já sabe criar um pedido aberto e imutável. Agora a aplicação precisa oferecer uma operação que receba um cliente e seus itens, coordene a criação do pedido e entregue o resultado a uma porta de saída.

Esta etapa introduz somente a fronteira de aplicação para o caso de uso. Não introduz HTTP, Controller, DTO, JPA, banco, configuração ou adapter de infraestrutura.

## Tarefa

Criar o caso de uso de criação de pedido com estas responsabilidades:

- Receber um cliente e uma lista de itens.
- Recusar uma lista vazia antes de criar ou salvar o pedido.
- Criar um `Pedido` novo usando o domínio existente.
- Adicionar os itens recebidos ao pedido, respeitando as regras do domínio.
- Salvar o pedido por uma interface de saída chamada `Pedidos`.
- Devolver o pedido retornado por `Pedidos`.

A aplicação deve expor um port de entrada chamado `CriarPedido`, ter uma implementação chamada `CriarPedidoService` e depender de um port de saída chamado `Pedidos`.

## Regras

- `CriarPedido` pertence à aplicação e representa a operação de criação.
- `CriarPedidoService` implementa `CriarPedido`.
- `Pedidos` pertence à aplicação e representa a capacidade de salvar um pedido.
- O caso de uso deve chamar `Pedidos` depois de construir o pedido completo.
- O retorno do caso de uso deve ser exatamente o pedido devolvido por `Pedidos`.
- A lista vazia deve ser recusada com erro de validação.
- Erros de validação dos itens continuam sendo responsabilidade do domínio.
- A aplicação não pode importar Spring, JPA, HTTP ou qualquer adapter.
- Não criar Controller, JPA, DTOs, configuração, banco, Docker ou commit.
- O teste deve fornecer uma implementação em memória de `Pedidos`.
- O fluxo deve continuar testável sem Spring e sem Postgres.

### Contrato comportamental

```text
dado um cliente e uma lista não vazia de itens válidos
quando CriarPedido for executado
então um Pedido novo é criado como ABERTO
e os itens são adicionados pelo domínio
e o pedido completo é enviado para Pedidos
e o pedido devolvido por Pedidos é retornado ao chamador
```

```text
dado um cliente e uma lista vazia de itens
quando CriarPedido for executado
então a operação é recusada
e nenhum pedido é criado
e Pedidos não é chamado
```

## Definição de pronto

- Existe um port de entrada `CriarPedido` na aplicação.
- Existe `CriarPedidoService` implementando o port de entrada.
- Existe um port de saída `Pedidos` na aplicação.
- Uma lista não vazia cria um pedido aberto com os itens recebidos.
- O caso de uso chama `Pedidos` uma vez com o pedido completo.
- O caso de uso devolve exatamente o pedido devolvido por `Pedidos`.
- Uma lista vazia gera erro de validação.
- Uma lista vazia não chama `Pedidos`.
- Os testes usam uma implementação em memória de `Pedidos`.
- Os testes não sobem Spring, não usam HTTP e não acessam Postgres.
- Os pacotes de aplicação não importam Spring, JPA, HTTP ou adapters.
- Não existem Controller, JPA, DTOs, configuração ou infraestrutura nova.

## Ambiguidade a confirmar antes da implementação

O caso de uso recebe `cliente`, mas o `Pedido` atual não possui esse dado. O plano propõe transformar `cliente` em um campo textual do `Pedido`, preservando-o no pedido criado e salvo. Se o cliente for apenas uma entrada transitória, será necessário definir como o caso de uso o utiliza sem descartá-lo.
