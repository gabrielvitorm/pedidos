package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PublicacaoPagamentoException;
import br.com.pedidos.api.application.port.in.RepublicarPagamentosPendentes;
import br.com.pedidos.api.application.port.out.PublicadorPagamentos;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;

import java.util.Objects;

/**
 * Recupera solicitações gravadas cuja publicação não foi confirmada, por exemplo quando o processo
 * caiu entre a gravação e a publicação. Republicar é seguro: o resultado repetido não tem efeito.
 */
public class RepublicarPagamentosPendentesService implements RepublicarPagamentosPendentes {

    private final SolicitacoesPagamento solicitacoes;
    private final PublicadorPagamentos publicador;

    public RepublicarPagamentosPendentesService(SolicitacoesPagamento solicitacoes, PublicadorPagamentos publicador) {
        this.solicitacoes = Objects.requireNonNull(solicitacoes);
        this.publicador = Objects.requireNonNull(publicador);
    }

    @Override
    public Resultado republicar() {
        int republicadas = 0;
        int falhas = 0;
        for (var solicitacao : solicitacoes.listarPendentesDePublicacao()) {
            try {
                publicador.publicar(solicitacao);
                solicitacoes.marcarPublicada(solicitacao.solicitacaoId());
                republicadas++;
            } catch (PublicacaoPagamentoException exception) {
                falhas++;
            }
        }
        return new Resultado(republicadas, falhas);
    }
}
