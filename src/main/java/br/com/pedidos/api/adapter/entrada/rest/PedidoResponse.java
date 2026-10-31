package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.StatusPedido;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PedidoResponse(
        UUID id,
        String clienteId,
        List<ItemResponse> itens,
        StatusPedido status,
        BigDecimal total
) {

    public PedidoResponse {
        itens = List.copyOf(itens);
    }

    public static PedidoResponse from(Pedido pedido) {
        var itens = pedido.itens().stream()
                .map(item -> new ItemResponse(item.produto(), item.quantidade(), item.precoUnitario()))
                .toList();
        return new PedidoResponse(pedido.id(), pedido.cliente(), itens, pedido.status(), pedido.total());
    }

    public record ItemResponse(String sku, int quantidade, BigDecimal precoUnitario) {
    }
}
