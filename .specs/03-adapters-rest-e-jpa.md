# Adapters REST e JPA

## Contexto

O domínio e o caso de uso de criação de pedido já existem sem dependência de frameworks. A próxima etapa deve conectar o port de saída `Pedidos` a uma persistência PostgreSQL por meio de um adapter JPA.

Esta spec organiza três provas independentes da evolução do sistema:

1. o banco PostgreSQL está disponível;
2. o pedido é persistido e recuperado pelo adapter JPA;
3. a aplicação pode ser exercitada por HTTP.

O checkpoint 4B cobre somente as duas primeiras provas. A prova HTTP fica registrada para um checkpoint posterior. Nenhum endpoint será criado no checkpoint 4B.

## Tarefa

No checkpoint 4B, criar o adapter de saída JPA que implementa o port de aplicação `Pedidos` e conecta o domínio ao PostgreSQL local.

O adapter deve:

- mapear `Pedido` para a tabela `pedido`;
- mapear seus itens para uma tabela de itens relacionada ao pedido;
- usar o ID `UUID` do domínio como ID persistido, sem gerar outro identificador;
- persistir cliente, status e itens;
- persistir em cada item `sku`, `quantidade` e `precoUnitario`;
- não criar coluna `total`, pois o total é derivado pelo domínio;
- recuperar um pedido pelo UUID em uma nova transação;
- devolver ao caso de uso um `Pedido` de domínio, nunca uma entidade JPA.

## Regras

- Adicionar somente `spring-boot-starter-data-jpa` e `postgresql` ao `pom.xml`, caso ainda não estejam presentes. Não duplicar dependências existentes.
- O domínio e a aplicação permanecem intactos quanto a imports de framework.
- Entidades JPA ficam em adapter de saída, não no domínio.
- O mapper entre entidades JPA e domínio deve ser manual.
- Não usar Lombok nem MapStruct.
- O adapter deve usar transação para carregar o pedido e seus itens antes de fechar a sessão.
- Configurar `spring.jpa.open-in-view=false`.
- Configurar `application.yaml` para o PostgreSQL local do compose: banco `pedidos`, usuário `pedidos`, senha `pedidos` e porta configurável.
- Não criar Controller, endpoint, DTO, migration, H2 ou infraestrutura adicional.
- Não remover containers ou volumes existentes.
- Usar execução explícita para os testes de integração `*IT`; os testes unitários devem continuar executáveis sem banco.

## Provas

### Prova 1: banco disponível

O serviço `postgres` do `docker-compose.yml` deve estar pronto. A prova deve confirmar `pg_isready` e executar `SELECT 1` dentro do serviço.

Se o banco não estiver pronto, o checkpoint deve parar e registrar o erro. Não iniciar ou remover outros serviços.

### Prova 2: persistência pelo adapter

Um teste de integração explícito deve:

1. criar o pedido do cliente `c-1`;
2. adicionar `CAFE-500`, quantidade `2`, preço unitário `18.90`;
3. salvar pelo `PedidosJpaAdapter`;
4. recuperar pelo UUID em uma nova transação;
5. verificar cliente, UUID, status `ABERTO`, item, quantidade, preço e total derivado `37.80`;
6. consultar a tabela e confirmar que não existe coluna `total`.

O teste deve ser `PedidosJpaAdapterIT` e não deve rodar na execução unitária padrão.

### Prova 3: HTTP

Para o checkpoint 4C, o endpoint `POST /pedidos` recebe e devolve JSON.

Requisição válida:

```json
{
  "clienteId": "c-1",
  "itens": [
    {
      "sku": "CAFE-500",
      "quantidade": 2,
      "precoUnitario": 18.90
    }
  ]
}
```

Resposta de sucesso: HTTP `201 Created`.

```json
{
  "id": "<uuid>",
  "clienteId": "c-1",
  "itens": [
    {
      "sku": "CAFE-500",
      "quantidade": 2,
      "precoUnitario": 18.90
    }
  ],
  "status": "ABERTO",
  "total": 37.80
}
```

Preços e total são números decimais JSON, não strings. `id` é o UUID gerado
por `Pedido.novo`; `total` é derivado dos itens.

Campos obrigatórios ausentes, formato inválido ou JSON malformado resultam em
HTTP `400`. A validação HTTP cobre presença e formato. Quantidade e preço
positivos continuam sendo regra do domínio; quantidade zero ou preço não
positivo resultam em `422`.

Uma lista de itens vazia resulta em `422`. As respostas de erro devem conter
uma mensagem. `ItemInvalidoException` e `PedidoSemItensException` são
exceções sem dependência de framework e são traduzidas pelo
`PedidoExceptionHandler` para `422`.

O controller depende somente do port de entrada `CriarPedido`. A composição
com `CriarPedidoService` ocorre em `CasosDeUsoConfig`. Não serão criados
outros endpoints. Esta seção substitui a reserva HTTP do checkpoint 4B.

## Definição de pronto

- O compose fornece PostgreSQL 16 no serviço `postgres`.
- `pg_isready` confirma o banco `pedidos` disponível.
- `SELECT 1` retorna `1`.
- `PedidoJpaEntity` e `ItemJpaEntity` representam as tabelas de persistência.
- O repository Spring Data JPA é usado somente pelo adapter.
- O mapper manual converte nos dois sentidos entre entidades e domínio.
- `PedidosJpaAdapter` implementa `Pedidos`.
- O pedido é salvo com o UUID do domínio.
- O pedido é recuperado por UUID em nova transação.
- O cenário `c-1` / `CAFE-500` / `2 × 18.90` retorna total derivado `37.80`.
- Não existe coluna persistida `total`.
- `spring.jpa.open-in-view=false` está configurado.
- Testes unitários passam sem banco.
- `PedidosJpaAdapterIT` passa somente quando executado explicitamente contra o PostgreSQL.
- O teste de contexto gerado é adaptado para a convenção `*IT`, sem ser apagado ou desativado.
- Domínio e aplicação não importam Spring, JPA, HTTP ou adapters.
- Não existem Controller, H2, DTOs, MapStruct ou Lombok novos.
