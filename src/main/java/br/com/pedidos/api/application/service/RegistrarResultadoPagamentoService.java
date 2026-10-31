package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.ResultadoPagamentoInvalidoException;
import br.com.pedidos.api.application.port.in.RegistrarResultadoPagamento;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.domain.model.Pedido;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class RegistrarResultadoPagamentoService implements RegistrarResultadoPagamento {

    private final Pedidos pedidos;
    private final SolicitacoesPagamento solicitacoes;

    public RegistrarResultadoPagamentoService(Pedidos pedidos, SolicitacoesPagamento solicitacoes) {
        this.pedidos = Objects.requireNonNull(pedidos);
        this.solicitacoes = Objects.requireNonNull(solicitacoes);
    }

    @Override
    public void registrar(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, boolean aprovado, String motivo) {
        var solicitacao = solicitacoes.buscarPorId(solicitacaoId)
                .orElseThrow(() -> new ResultadoPagamentoInvalidoException(
                        "Resultado de solicitacao desconhecida: " + solicitacaoId));
        if (!solicitacao.pedidoId().equals(pedidoId) || solicitacao.valor().compareTo(valor) != 0) {
            throw new ResultadoPagamentoInvalidoException(
                    "Resultado diverge do pedido ou valor registrado na solicitacao " + solicitacaoId);
        }
        if (solicitacao.resolvida()) {
            if (solicitacao.aprovado() == aprovado) {
                return;
            }
            throw new ResultadoPagamentoInvalidoException(
                    "Resultado conflitante com o ja aplicado na solicitacao " + solicitacaoId);
        }

        var pedido = pedidos.buscarPorId(pedidoId)
                .orElseThrow(() -> new ResultadoPagamentoInvalidoException("Pedido inexistente: " + pedidoId));
        solicitacoes.registrarResultado(aplicar(pedido, aprovado), solicitacao.resolver(aprovado, motivo));
    }

    private Pedido aplicar(Pedido pedido, boolean aprovado) {
        try {
            return aprovado ? pedido.aprovarPagamento() : pedido.recusarPagamento();
        } catch (IllegalStateException exception) {
            throw new ResultadoPagamentoInvalidoException(
                    "Pedido " + pedido.id() + " nao esta em pagamento: " + pedido.status());
        }
    }
}
