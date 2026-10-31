package br.com.pedidos.api.application.exception;

public class PedidoFechadoException extends IllegalStateException {

    public PedidoFechadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
