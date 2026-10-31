package br.com.pedidos.api.adapter.out.persistence;

import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "solicitacao_pagamento")
public class SolicitacaoPagamentoJpaEntity {

    @Id
    @Column(name = "solicitacao_id", nullable = false)
    private UUID solicitacaoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoJpaEntity pedido;

    @Column(name = "pedido_id", nullable = false, insertable = false, updatable = false)
    private UUID pedidoId;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "cenario", nullable = false)
    private CenarioPagamento cenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SolicitacaoPagamento.Status status;

    @Column(name = "aprovado")
    private Boolean aprovado;

    @Column(name = "motivo")
    private String motivo;

    protected SolicitacaoPagamentoJpaEntity() {
    }

    SolicitacaoPagamentoJpaEntity(SolicitacaoPagamento solicitacao, PedidoJpaEntity pedido) {
        this.solicitacaoId = solicitacao.solicitacaoId();
        this.pedido = pedido;
        this.pedidoId = solicitacao.pedidoId();
        this.valor = solicitacao.valor();
        this.cenario = solicitacao.cenario();
        this.status = solicitacao.status();
    }

    SolicitacaoPagamento toDomain() {
        return new SolicitacaoPagamento(solicitacaoId, pedidoId, valor, cenario, status, aprovado, motivo);
    }
}
