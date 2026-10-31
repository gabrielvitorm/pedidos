package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.application.port.in.SolicitarPagamento;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PagamentoController {

    private final SolicitarPagamento solicitarPagamento;

    public PagamentoController(SolicitarPagamento solicitarPagamento) {
        this.solicitarPagamento = solicitarPagamento;
    }

    /**
     * 202 indica solicitação aceita, persistida, confirmada e roteada pelo broker; nunca pagamento aprovado.
     */
    @PostMapping("/pedidos/{pedidoId}/pagamentos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SolicitacaoPagamentoResponse solicitar(@PathVariable UUID pedidoId,
                                                  @Valid @RequestBody SolicitarPagamentoRequest request) {
        var solicitacao = solicitarPagamento.solicitar(pedidoId, request.solicitacaoId(), request.cenario());
        return SolicitacaoPagamentoResponse.from(solicitacao);
    }
}
