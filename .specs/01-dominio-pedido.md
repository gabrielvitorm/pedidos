/# Domínio inicial de pedidos

## Contexto

O serviço precisa começar pelo comportamento essencial de um pedido, antes da criação de adaptadores, persistência ou endpoints. A primeira unidade de negócio representa um pedido que recebe itens, calcula seu total e percorre um ciclo de vida simples: começa aberto e pode ser pago ou cancelado.

O objetivo desta mudança é estabelecer um núcleo de domínio pequeno, previsível, imutável do ponto de vista de quem o utiliza e independente de frameworks. O exemplo da aula é: o cliente `c-1` compra `CAFE-500`, com 2 unidades a `18.90`, resultando em `37.80`.

## Tarefa

Descrever e, em uma etapa posterior aprovada, implementar o comportamento inicial de pedidos com os conceitos `Pedido`, `ItemPedido` e `StatusPedido`.

O comportamento esperado é:

- Criar um pedido novo produz um rascunho vazio, com identificador UUID e status `ABERTO`.
- Um item representa um produto, uma quantidade e um preço unitário. Quantidade e preço devem ser positivos.
- O total do pedido é derivado dos itens: soma de quantidade multiplicada pelo preço unitário de cada item. Ele não é informado nem armazenado como um valor independente.
- Adicionar um item a um pedido aberto devolve outro pedido contendo os itens anteriores e o novo item. O pedido original permanece inalterado.
- Um pedido pago ou cancelado não aceita novos itens.
- Um pedido aberto pode ser pago ou cancelado.
- Tentativas de pagar ou cancelar um pedido que não esteja aberto são recusadas.
- Coleções expostas pelo pedido e pelos itens não podem ser modificadas por código externo.

### Exemplo da aula

```text
dado um pedido novo para o cliente c-1
quando for adicionado o produto CAFE-500 com quantidade 2 e preço unitário 18.90
então o pedido resultante terá total 37.80
```

## Regras

### Regras de negócio

1. O pedido novo começa `ABERTO`, vazio e com UUID.
2. Quantidade deve ser maior que zero.
3. Preço unitário deve ser maior que zero.
4. O total deve ser derivado pela soma de `quantidade × preço unitário`.
5. Valores monetários devem usar `BigDecimal`; não usar `double` ou `float` para dinheiro.
6. Adicionar item só é permitido enquanto o pedido estiver `ABERTO`.
7. Pagar só é permitido enquanto o pedido estiver `ABERTO`.
8. Cancelar só é permitido enquanto o pedido estiver `ABERTO`.
9. A operação de adicionar item devolve um novo objeto; não altera o pedido de origem.
10. O pedido pago ou cancelado não pode receber itens.
11. Depois de pago ou cancelado, o pedido não pode ser pago ou cancelado novamente.

### Regras técnicas e de modelagem

- O domínio inicial deve conter os conceitos `Pedido`, `ItemPedido` e `StatusPedido`.
- Os valores do domínio devem ser modelados com `records`, incluindo a validação necessária em sua criação.
- A construção de coleções deve fazer cópia defensiva, e as coleções observáveis externamente devem ser somente para leitura.
- O pedido novo deve ser criado pela operação `Pedido.novo`, que gera o UUID, define status `ABERTO` e inicia a lista vazia.
- A operação de adicionar item deve devolver outro pedido, preservando o UUID, o status e os itens anteriores, acrescidos do novo item.
- O domínio e a aplicação não podem depender de Spring, JPA ou detalhes de infraestrutura.
- Não criar endpoints, repositórios, entidades JPA, migrações, Docker ou outra infraestrutura nesta etapa.
- O domínio não deve introduzir dependência nova.

## Definição de pronto

Cada regra abaixo deve ter um caso de teste verificável, incluindo os cenários de erro:

| Caso de teste | Dado | Quando | Então |
|---|---|---|---|
| Cria pedido novo | Nenhum pedido existente | Criar um pedido novo | O resultado tem UUID, status `ABERTO` e lista vazia |
| Quantidade positiva aceita | Um produto e quantidade `2` | Criar um item | O item é criado |
| Quantidade zero é recusada | Um produto e quantidade `0` | Criar um item | A operação falha com erro de validação |
| Quantidade negativa é recusada | Um produto e quantidade menor que zero | Criar um item | A operação falha com erro de validação |
| Preço positivo aceita | Preço unitário `18.90` | Criar um item | O item é criado com valor monetário decimal |
| Preço zero é recusado | Preço unitário `0` | Criar um item | A operação falha com erro de validação |
| Preço negativo é recusado | Preço unitário menor que zero | Criar um item | A operação falha com erro de validação |
| Total é derivado | Um item com quantidade `2` e preço `18.90` | Consultar o total | O total é `37.80` |
| Total soma vários itens | Mais de um item válido | Consultar o total | O total é a soma de cada quantidade multiplicada pelo respectivo preço |
| Exemplo da aula | Cliente `c-1`, produto `CAFE-500`, quantidade `2`, preço `18.90` | Adicionar o item e consultar o total | O total é `37.80` |
| Adiciona item em pedido aberto | Pedido `ABERTO` | Adicionar item válido | É devolvido outro pedido com o item |
| Pedido original permanece inalterado | Pedido aberto sem o novo item | Adicionar um item | O pedido original continua vazio e o novo pedido contém o item |
| Adicionar item em pedido pago é recusado | Pedido `PAGO` | Adicionar item | A operação falha com erro de transição |
| Adicionar item em pedido cancelado é recusado | Pedido `CANCELADO` | Adicionar item | A operação falha com erro de transição |
| Pedido aberto pode ser pago | Pedido `ABERTO` | Pagar | O resultado fica `PAGO` |
| Pedido aberto pode ser cancelado | Pedido `ABERTO` | Cancelar | O resultado fica `CANCELADO` |
| Pagar pedido pago é recusado | Pedido `PAGO` | Pagar novamente | A operação falha com erro de transição |
| Cancelar pedido pago é recusado | Pedido `PAGO` | Cancelar | A operação falha com erro de transição |
| Pagar pedido cancelado é recusado | Pedido `CANCELADO` | Pagar | A operação falha com erro de transição |
| Cancelar pedido cancelado é recusado | Pedido `CANCELADO` | Cancelar novamente | A operação falha com erro de transição |
| Lista de itens não pode ser alterada externamente | Pedido com itens | Tentar adicionar, remover ou substituir elemento pela coleção exposta | A alteração é recusada e o pedido permanece igual |
| Cópia defensiva protege a origem | Lista mutável usada na criação de um item/pedido | Alterar a lista original depois da criação | O objeto de domínio não muda |
| Cópia defensiva protege o resultado | Pedido original e lista de itens conhecida | Alterar referências de entrada após adicionar item | O pedido original e o novo pedido mantêm seus próprios estados |
| Dinheiro não usa ponto flutuante | Qualquer item monetário válido | Inspecionar o tipo do preço e do total | Ambos usam `BigDecimal` e o cálculo preserva `37.80` no exemplo |
| Domínio sem frameworks | Código do domínio | Compilar e executar seus testes sem infraestrutura | Não há dependência de Spring ou JPA no domínio/aplicação |

Além desses casos, a verificação deve confirmar que não foram criados endpoints, persistência, infraestrutura ou dependências novas.

## Ambiguidades a decidir antes da implementação

- **Tipos e mensagens de erro:** a regra exige que as operações sejam recusadas, mas ainda não define classes nem mensagens específicas para validação e transições inválidas. Recomenda-se decidir se haverá exceções de domínio distintas ou um único tipo de erro.
- **Identidade do produto:** `CAFE-500` foi usado como código textual no exemplo; ainda não está definido se o item terá somente esse código, também uma descrição, ou uma referência a outro objeto de produto.
- **Cliente do pedido:** `c-1` aparece no exemplo, mas não foi definido se o pedido deve armazenar um identificador de cliente nesta primeira versão.
- **Escala de `BigDecimal`:** o exemplo requer a representação monetária `18.90` e `37.80`, mas ainda não foi definido se a criação deve exigir duas casas decimais ou apenas preservar a escala informada.
- **Operações de pagamento e cancelamento:** está definido que devolvem um novo estado, mas ainda não foi especificado se também devem devolver um novo objeto, como ocorre com `adicionarItem`, ou se essa regra de imutabilidade vale para todas as transições.
