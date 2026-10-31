package br.com.pedidos.api.application.port.out;

import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;

import java.util.Optional;
import java.util.UUID;

public interface SolicitacoesPagamento {

    Optional<SolicitacaoPagamento> buscarPorId(UUID solicitacaoId);

    /**
     * Grava o pedido em pagamento e a solicitação pendente de forma atômica.
     */
    void registrar(Pedido pedidoEmPagamento, SolicitacaoPagamento solicitacao);

    SolicitacaoPagamento marcarPublicada(UUID solicitacaoId);
}
