package br.com.pedidos.api.adapter.out.persistence;

import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;

import java.util.List;

public class PedidoJpaMapper {

    PedidoJpaEntity toEntity(Pedido pedido) {
        var entity = new PedidoJpaEntity(pedido.id(), pedido.cliente(), pedido.status());
        pedido.itens().forEach(item -> entity.adicionarItem(
                new ItemJpaEntity(pedido.id(), item.produto(), item.quantidade(), item.precoUnitario())));
        return entity;
    }

    Pedido toDomain(PedidoJpaEntity entity) {
        var itens = entity.itens().stream()
                .map(item -> new ItemPedido(item.sku(), item.quantidade(), item.precoUnitario()))
                .toList();
        return new Pedido(entity.id(), entity.cliente(), entity.status(), List.copyOf(itens));
    }
}
