package br.com.pedidos.api.application.exception;

public class PedidoSemItensException extends IllegalArgumentException {

    public PedidoSemItensException(String message) {
        super(message);
    }
}
