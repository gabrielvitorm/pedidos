package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import br.com.pedidos.api.domain.model.StatusPedido;

import java.util.UUID;

public record SolicitacaoPagamentoResponse(UUID solicitacaoId, UUID pedidoId, StatusPedido status) {

    public static SolicitacaoPagamentoResponse from(SolicitacaoPagamento solicitacao) {
        return new SolicitacaoPagamentoResponse(
                solicitacao.solicitacaoId(), solicitacao.pedidoId(), StatusPedido.EM_PAGAMENTO);
    }
}
