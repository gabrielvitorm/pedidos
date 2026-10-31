package br.com.pedidos.api.adapter.out.mensageria;

import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;

import java.math.BigDecimal;
import java.util.UUID;

public record SolicitacaoPagamentoMensagem(
        int versao,
        UUID solicitacaoId,
        UUID pedidoId,
        BigDecimal valor,
        CenarioPagamento cenario
) {

    public static final int VERSAO_ATUAL = 1;

    public static SolicitacaoPagamentoMensagem from(SolicitacaoPagamento solicitacao) {
        return new SolicitacaoPagamentoMensagem(VERSAO_ATUAL, solicitacao.solicitacaoId(), solicitacao.pedidoId(),
                solicitacao.valor(), solicitacao.cenario());
    }
}
