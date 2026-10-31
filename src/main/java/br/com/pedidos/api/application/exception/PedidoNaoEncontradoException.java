package br.com.pedidos.api.application.exception;

import java.util.UUID;

public class PedidoNaoEncontradoException extends RuntimeException {

    public PedidoNaoEncontradoException(UUID id) {
        super("Pedido não encontrado: " + id);
    }
}
