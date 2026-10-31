package br.com.pedidos.api.adapter.out.persistence;

import br.com.pedidos.api.application.exception.PedidoEstadoIncompativelException;
import br.com.pedidos.api.application.exception.ResultadoPagamentoInvalidoException;
import br.com.pedidos.api.application.exception.SolicitacaoConflitanteException;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import br.com.pedidos.api.domain.model.StatusPedido;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class SolicitacoesPagamentoJpaAdapter implements SolicitacoesPagamento {

    private static final String CHAVE_SOLICITACAO = "solicitacao_pagamento_pkey";

    private final SolicitacaoPagamentoJpaRepository repository;
    private final PedidoJpaRepository pedidos;
    private final EntityManager entityManager;

    public SolicitacoesPagamentoJpaAdapter(SolicitacaoPagamentoJpaRepository repository,
                                           PedidoJpaRepository pedidos,
                                           EntityManager entityManager) {
        this.repository = repository;
        this.pedidos = pedidos;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<SolicitacaoPagamento> buscarPorId(UUID solicitacaoId) {
        return repository.findById(solicitacaoId).map(SolicitacaoPagamentoJpaEntity::toDomain);
    }

    @Transactional
    @Override
    public void registrar(Pedido pedidoEmPagamento, SolicitacaoPagamento solicitacao) {
        if (pedidoEmPagamento.status() != StatusPedido.EM_PAGAMENTO
                || !pedidoEmPagamento.id().equals(solicitacao.pedidoId())) {
            throw new IllegalArgumentException("Registro exige o pedido da solicitacao em pagamento");
        }
        var alterados = pedidos.alterarStatus(pedidoEmPagamento.id(), StatusPedido.ABERTO, StatusPedido.EM_PAGAMENTO);
        if (alterados != 1) {
            throw new PedidoEstadoIncompativelException("Pedido deve estar aberto", null);
        }
        try {
            entityManager.persist(new SolicitacaoPagamentoJpaEntity(
                    solicitacao, pedidos.getReferenceById(pedidoEmPagamento.id())));
            entityManager.flush();
        } catch (ConstraintViolationException exception) {
            if (CHAVE_SOLICITACAO.equals(exception.getConstraintName())) {
                throw new SolicitacaoConflitanteException(solicitacao.solicitacaoId());
            }
            throw exception;
        }
    }

    @Transactional
    @Override
    public SolicitacaoPagamento marcarPublicada(UUID solicitacaoId) {
        if (repository.alterarStatus(solicitacaoId, SolicitacaoPagamento.Status.PUBLICADA) != 1) {
            throw new IllegalStateException("Solicitacao inexistente: " + solicitacaoId);
        }
        return repository.findById(solicitacaoId).orElseThrow().toDomain();
    }

    @Transactional
    @Override
    public void registrarResultado(Pedido pedidoAtualizado, SolicitacaoPagamento solicitacaoResolvida) {
        if (!solicitacaoResolvida.resolvida() || !pedidoAtualizado.id().equals(solicitacaoResolvida.pedidoId())) {
            throw new IllegalArgumentException("Registro exige a solicitacao resolvida do proprio pedido");
        }
        var alterados = pedidos.alterarStatus(
                pedidoAtualizado.id(), StatusPedido.EM_PAGAMENTO, pedidoAtualizado.status());
        if (alterados != 1) {
            throw new ResultadoPagamentoInvalidoException(
                    "Pedido " + pedidoAtualizado.id() + " nao esta mais em pagamento");
        }
        var registrados = repository.registrarResultado(solicitacaoResolvida.solicitacaoId(),
                solicitacaoResolvida.aprovado(), solicitacaoResolvida.motivo());
        if (registrados != 1) {
            throw new ResultadoPagamentoInvalidoException(
                    "Solicitacao " + solicitacaoResolvida.solicitacaoId() + " ja possui resultado ou nao existe");
        }
    }
}
