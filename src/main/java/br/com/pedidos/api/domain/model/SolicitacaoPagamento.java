package br.com.pedidos.api.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record SolicitacaoPagamento(
        UUID solicitacaoId,
        UUID pedidoId,
        BigDecimal valor,
        CenarioPagamento cenario,
        Status status
) {

    public enum Status {
        PENDENTE_PUBLICACAO,
        PUBLICADA
    }

    public SolicitacaoPagamento {
        if (solicitacaoId == null) {
            throw new IllegalArgumentException("Solicitacao deve possuir identificador");
        }
        if (pedidoId == null) {
            throw new IllegalArgumentException("Solicitacao deve possuir pedido");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor da solicitacao deve ser positivo");
        }
        if (cenario == null) {
            throw new IllegalArgumentException("Solicitacao deve possuir cenario");
        }
        if (status == null) {
            throw new IllegalArgumentException("Solicitacao deve possuir status");
        }
    }

    public static SolicitacaoPagamento pendente(UUID solicitacaoId, UUID pedidoId, BigDecimal valor,
                                                CenarioPagamento cenario) {
        return new SolicitacaoPagamento(solicitacaoId, pedidoId, valor, cenario, Status.PENDENTE_PUBLICACAO);
    }

    public SolicitacaoPagamento publicada() {
        return new SolicitacaoPagamento(solicitacaoId, pedidoId, valor, cenario, Status.PUBLICADA);
    }

    public boolean pendenteDePublicacao() {
        return status == Status.PENDENTE_PUBLICACAO;
    }

    public boolean mesmoConteudo(UUID outroPedidoId, CenarioPagamento outroCenario) {
        return pedidoId.equals(outroPedidoId) && cenario == outroCenario;
    }
}
