package br.com.pedidos.api.application.exception;

public class PublicacaoPagamentoException extends RuntimeException {

    public PublicacaoPagamentoException(String message) {
        super(message);
    }

    public PublicacaoPagamentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
