package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.ItemInvalidoException;
import br.com.pedidos.api.application.exception.PedidoFechadoException;
import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class AdicionarItemService implements AdicionarItem {

    private final Pedidos pedidos;

    public AdicionarItemService(Pedidos pedidos) {
        this.pedidos = Objects.requireNonNull(pedidos);
    }

    @Override
    public Pedido adicionar(UUID pedidoId, String sku, int quantidade, BigDecimal precoUnitario) {
        var pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));
        var item = criarItem(sku, quantidade, precoUnitario);
        var pedidoAtualizado = adicionarAoPedido(pedido, item);
        return pedidos.salvar(pedidoAtualizado);
    }

    private ItemPedido criarItem(String sku, int quantidade, BigDecimal precoUnitario) {
        try {
            return new ItemPedido(sku, quantidade, precoUnitario);
        } catch (IllegalArgumentException exception) {
            throw new ItemInvalidoException(exception.getMessage(), exception);
        }
    }

    private Pedido adicionarAoPedido(Pedido pedido, ItemPedido item) {
        try {
            return pedido.adicionarItem(item);
        } catch (IllegalStateException exception) {
            throw new PedidoFechadoException(exception.getMessage(), exception);
        }
    }
}
