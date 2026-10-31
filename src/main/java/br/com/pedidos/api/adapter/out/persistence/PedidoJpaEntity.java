package br.com.pedidos.api.adapter.out.persistence;

import br.com.pedidos.api.domain.model.StatusPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedido")
public class PedidoJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "cliente", nullable = false)
    private String cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusPedido status;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.posicao")
    private List<ItemJpaEntity> itens = new ArrayList<>();

    protected PedidoJpaEntity() {
    }

    PedidoJpaEntity(UUID id, String cliente, StatusPedido status) {
        this.id = id;
        this.cliente = cliente;
        this.status = status;
    }

    void adicionarItem(ItemJpaEntity item) {
        item.vincularPedido(this);
        itens.add(item);
    }

    UUID id() {
        return id;
    }

    String cliente() {
        return cliente;
    }

    StatusPedido status() {
        return status;
    }

    List<ItemJpaEntity> itens() {
        return itens;
    }
}
