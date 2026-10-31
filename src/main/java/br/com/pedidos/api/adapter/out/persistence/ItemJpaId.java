package br.com.pedidos.api.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class ItemJpaId implements Serializable {

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Column(name = "posicao", nullable = false)
    private int posicao;

    protected ItemJpaId() {
    }

    ItemJpaId(UUID pedidoId, int posicao) {
        this.pedidoId = pedidoId;
        this.posicao = posicao;
    }

    UUID pedidoId() {
        return pedidoId;
    }

    int posicao() {
        return posicao;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ItemJpaId other)) {
            return false;
        }
        return pedidoId.equals(other.pedidoId) && posicao == other.posicao;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(pedidoId, posicao);
    }
}
