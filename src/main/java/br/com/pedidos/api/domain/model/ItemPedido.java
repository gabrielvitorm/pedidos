package br.com.pedidos.api.domain.model;

import java.math.BigDecimal;

public record ItemPedido(String produto, int quantidade, BigDecimal precoUnitario) {

    public ItemPedido {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        if (precoUnitario == null || precoUnitario.signum() <= 0) {
            throw new IllegalArgumentException("Preco unitario deve ser positivo");
        }
    }
}
