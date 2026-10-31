package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.port.in.ConsultarPedido;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.model.Pedido;

import java.util.Objects;
import java.util.UUID;

public class ConsultarPedidoService implements ConsultarPedido {

    private final Pedidos pedidos;

    public ConsultarPedidoService(Pedidos pedidos) {
        this.pedidos = Objects.requireNonNull(pedidos);
    }

    @Override
    public Pedido consultar(UUID id) {
        return pedidos.buscarPorId(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }
}
