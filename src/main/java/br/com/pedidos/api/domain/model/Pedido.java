package br.com.pedidos.api.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Pedido(UUID id, String cliente, StatusPedido status, List<ItemPedido> itens) {

    public Pedido {
        if (id == null) {
            throw new IllegalArgumentException("Pedido deve possuir identificador");
        }
        if (cliente == null || cliente.isBlank()) {
            throw new IllegalArgumentException("Pedido deve possuir cliente");
        }
        if (status == null) {
            throw new IllegalArgumentException("Pedido deve possuir status");
        }
        if (itens == null) {
            throw new IllegalArgumentException("Pedido deve possuir lista de itens");
        }
        itens = List.copyOf(itens);
    }

    public static Pedido novo(String cliente) {
        return new Pedido(UUID.randomUUID(), cliente, StatusPedido.ABERTO, List.of());
    }

    public Pedido adicionarItem(ItemPedido item) {
        exigirAberto();
        var novosItens = new ArrayList<>(itens);
        novosItens.add(item);
        return new Pedido(id, cliente, status, novosItens);
    }

    public BigDecimal total() {
        return itens.stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Pedido pagar() {
        exigirAberto();
        return new Pedido(id, cliente, StatusPedido.PAGO, itens);
    }

    public Pedido cancelar() {
        exigirAberto();
        return new Pedido(id, cliente, StatusPedido.CANCELADO, itens);
    }

    private void exigirAberto() {
        if (status != StatusPedido.ABERTO) {
            throw new IllegalStateException("Pedido deve estar aberto");
        }
    }
}
