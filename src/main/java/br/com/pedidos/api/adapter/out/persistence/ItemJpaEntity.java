package br.com.pedidos.api.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_pedido")
public class ItemJpaEntity {

    @EmbeddedId
    private ItemJpaId id;

    @MapsId("pedidoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoJpaEntity pedido;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemJpaEntity() {
    }

    ItemJpaEntity(UUID pedidoId, int posicao, String sku, int quantidade, BigDecimal precoUnitario) {
        this.id = new ItemJpaId(pedidoId, posicao);
        this.sku = sku;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    void vincularPedido(PedidoJpaEntity pedido) {
        this.pedido = pedido;
        this.id = new ItemJpaId(pedido.id(), id.posicao());
    }

    String sku() {
        return sku;
    }

    int quantidade() {
        return quantidade;
    }

    BigDecimal precoUnitario() {
        return precoUnitario;
    }
}
