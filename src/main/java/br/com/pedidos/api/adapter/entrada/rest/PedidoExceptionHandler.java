package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.application.exception.ItemInvalidoException;
import br.com.pedidos.api.application.exception.PedidoSemItensException;
import br.com.pedidos.api.application.exception.PedidoEstadoIncompativelException;
import br.com.pedidos.api.application.exception.PedidoFechadoException;
import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.exception.PublicacaoPagamentoException;
import br.com.pedidos.api.application.exception.SolicitacaoConflitanteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler({PedidoFechadoException.class, PedidoEstadoIncompativelException.class,
            SolicitacaoConflitanteException.class})
    public ResponseEntity<ErroResponse> tratarConflito(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(exception.getMessage()));
    }

    @ExceptionHandler(PublicacaoPagamentoException.class)
    public ResponseEntity<ErroResponse> tratarPublicacaoIndisponivel(PublicacaoPagamentoException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErroResponse("Pagamento nao pode ser solicitado agora; repita com a mesma solicitacaoId"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponse> tratarRequisicaoInvalida(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse("Requisição inválida"));
    }

    public record ErroResponse(String mensagem) {
    }
}
