package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.application.exception.ItemInvalidoException;
import br.com.pedidos.api.application.exception.PedidoSemItensException;
import br.com.pedidos.api.application.exception.PedidoFechadoException;
import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PedidoExceptionHandler {

    @ExceptionHandler({ItemInvalidoException.class, PedidoSemItensException.class})
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ErroResponse(exception.getMessage()));
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarPedidoNaoEncontrado(PedidoNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(exception.getMessage()));
    }

    @ExceptionHandler(PedidoFechadoException.class)
    public ResponseEntity<ErroResponse> tratarPedidoFechado(PedidoFechadoException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(exception.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErroResponse> tratarRequisicaoInvalida(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse("Requisição inválida"));
    }

    public record ErroResponse(String mensagem) {
    }
}
