package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PedidoEstadoIncompativelException;
import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.exception.SolicitacaoConflitanteException;
import br.com.pedidos.api.application.port.in.SolicitarPagamento;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.PublicadorPagamentos;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;

import java.util.Objects;
import java.util.UUID;

public class SolicitarPagamentoService implements SolicitarPagamento {

    private final Pedidos pedidos;
    private final SolicitacoesPagamento solicitacoes;
    private final PublicadorPagamentos publicador;

    public SolicitarPagamentoService(Pedidos pedidos, SolicitacoesPagamento solicitacoes,
                                     PublicadorPagamentos publicador) {
        this.pedidos = Objects.requireNonNull(pedidos);
        this.solicitacoes = Objects.requireNonNull(solicitacoes);
        this.publicador = Objects.requireNonNull(publicador);
    }

    @Override
    public SolicitacaoPagamento solicitar(UUID pedidoId, UUID solicitacaoId, CenarioPagamento cenario) {
        var existente = solicitacoes.buscarPorId(solicitacaoId);
        if (existente.isPresent()) {
            return repetir(existente.get(), pedidoId, cenario);
        }

        var pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));
        var pedidoEmPagamento = iniciarPagamento(pedido);
        var solicitacao = SolicitacaoPagamento.pendente(solicitacaoId, pedidoId, pedido.total(), cenario);
        solicitacoes.registrar(pedidoEmPagamento, solicitacao);
        return publicar(solicitacao);
    }

    private SolicitacaoPagamento repetir(SolicitacaoPagamento existente, UUID pedidoId, CenarioPagamento cenario) {
        if (!existente.mesmoConteudo(pedidoId, cenario)) {
            throw new SolicitacaoConflitanteException(existente.solicitacaoId());
        }
        if (existente.pendenteDePublicacao()) {
            return publicar(existente);
        }
        return existente;
    }

    private Pedido iniciarPagamento(Pedido pedido) {
        try {
            return pedido.iniciarPagamento();
        } catch (IllegalStateException exception) {
            throw new PedidoEstadoIncompativelException(exception.getMessage(), exception);
        }
    }

    private SolicitacaoPagamento publicar(SolicitacaoPagamento solicitacao) {
        publicador.publicar(solicitacao);
        return solicitacoes.marcarPublicada(solicitacao.solicitacaoId());
    }
}
