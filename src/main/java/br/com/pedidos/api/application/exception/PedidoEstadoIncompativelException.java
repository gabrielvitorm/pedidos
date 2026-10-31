package br.com.pedidos.api.application.exception;

public class PedidoEstadoIncompativelException extends IllegalStateException {

    public PedidoEstadoIncompativelException(String message, Throwable cause) {
        super(message, cause);
    }
}
