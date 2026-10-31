package br.com.pedidos.api.application.exception;

public class ItemInvalidoException extends RuntimeException {

    public ItemInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
