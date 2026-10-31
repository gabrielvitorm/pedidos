package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.port.in.CriarPedido;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.exception.PedidoSemItensException;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;

import java.util.List;
import java.util.Objects;

public class CriarPedidoService implements CriarPedido {

    private final Pedidos pedidos;

    public CriarPedidoService(Pedidos pedidos) {
        this.pedidos = Objects.requireNonNull(pedidos);
    }

    @Override
    public Pedido criar(String cliente, List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoSemItensException("Pedido deve possuir ao menos um item");
        }

        var pedido = Pedido.novo(cliente);
        for (var item : itens) {
            pedido = pedido.adicionarItem(item);
        }
        return pedidos.salvar(pedido);
    }
}
