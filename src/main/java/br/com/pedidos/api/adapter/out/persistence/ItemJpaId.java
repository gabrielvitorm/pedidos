package br.com.pedidos.api.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class ItemJpaId implements Serializable {

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Column(name = "sku", nullable = false)
    private String sku;

    protected ItemJpaId() {
    }

    ItemJpaId(UUID pedidoId, String sku) {
        this.pedidoId = pedidoId;
        this.sku = sku;
    }

    UUID pedidoId() {
        return pedidoId;
    }

    String sku() {
        return sku;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ItemJpaId other)) {
            return false;
        }
        return pedidoId.equals(other.pedidoId) && sku.equals(other.sku);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pedidoId, sku);
    }
}
