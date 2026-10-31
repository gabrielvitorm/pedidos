package br.com.pedidos.api.application.exception;

/**
 * Resultado que não pode ser aplicado e não deve ser reprocessado: solicitação desconhecida,
 * dados divergentes do registrado ou resultado oposto a um já aplicado.
 */
public class ResultadoPagamentoInvalidoException extends RuntimeException {

    public ResultadoPagamentoInvalidoException(String message) {
        super(message);
    }
}
