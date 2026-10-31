package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.ResultadoPagamentoInvalidoException;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import br.com.pedidos.api.domain.model.StatusPedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrarResultadoPagamentoServiceTest {

    private static final BigDecimal VALOR = new BigDecimal("37.80");

    private final PedidosEmMemoria pedidos = new PedidosEmMemoria();
    private final SolicitacoesEmMemoria solicitacoes = new SolicitacoesEmMemoria(pedidos);
    private final RegistrarResultadoPagamentoService service =
            new RegistrarResultadoPagamentoService(pedidos, solicitacoes);

    @Test
    void aprovacaoDeixaPedidoPagoEGravaResultado() {
        var solicitacao = solicitacaoPublicada(CenarioPagamento.APROVAR);

        service.registrar(solicitacao.solicitacaoId(), solicitacao.pedidoId(), VALOR, true, "Aprovado pelo simulador");

        assertThat(pedidos.porId(solicitacao.pedidoId()).status()).isEqualTo(StatusPedido.PAGO);
        var gravada = solicitacoes.porId(solicitacao.solicitacaoId());
        assertThat(gravada.aprovado()).isTrue();
        assertThat(gravada.motivo()).isEqualTo("Aprovado pelo simulador");
    }

    @Test
    void recusaVoltaPedidoParaAbertoComMesmosItens() {
        var solicitacao = solicitacaoPublicada(CenarioPagamento.RECUSAR);
        var itensAntes = pedidos.porId(solicitacao.pedidoId()).itens();

        service.registrar(solicitacao.solicitacaoId(), solicitacao.pedidoId(), VALOR, false, "Recusado pelo simulador");

        var pedido = pedidos.porId(solicitacao.pedidoId());
        assertThat(pedido.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(pedido.itens()).isEqualTo(itensAntes);
        assertThat(solicitacoes.porId(solicitacao.solicitacaoId()).aprovado()).isFalse();
    }

    @Test
    void resultadoRepetidoNaoTemEfeito() {
        var solicitacao = solicitacaoPublicada(CenarioPagamento.APROVAR);
        service.registrar(solicitacao.solicitacaoId(), solicitacao.pedidoId(), VALOR, true, "Aprovado pelo simulador");

        service.registrar(solicitacao.solicitacaoId(), solicitacao.pedidoId(), VALOR, true, "Aprovado pelo simulador");

        assertThat(solicitacoes.registros).isEqualTo(1);
        assertThat(pedidos.porId(solicitacao.pedidoId()).status()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    void resultadoOpostoAoAplicadoEhRecusado() {
        var solicitacao = solicitacaoPublicada(CenarioPagamento.APROVAR);
        service.registrar(solicitacao.solicitacaoId(), solicitacao.pedidoId(), VALOR, true, "Aprovado pelo simulador");

        assertThatThrownBy(() -> service.registrar(solicitacao.solicitacaoId(), solicitacao.pedidoId(), VALOR,
                false, "Recusado"))
                .isInstanceOf(ResultadoPagamentoInvalidoException.class)
                .hasMessageContaining("conflitante");

        assertThat(solicitacoes.registros).isEqualTo(1);
        assertThat(pedidos.porId(solicitacao.pedidoId()).status()).isEqualTo(StatusPedido.PAGO);
        assertThat(solicitacoes.porId(solicitacao.solicitacaoId()).aprovado()).isTrue();
    }

    @Test
    void solicitacaoDesconhecidaEhRecusada() {
        assertThatThrownBy(() -> service.registrar(UUID.randomUUID(), UUID.randomUUID(), VALOR, true, "Aprovado"))
                .isInstanceOf(ResultadoPagamentoInvalidoException.class)
                .hasMessageContaining("desconhecida");

        assertThat(solicitacoes.registros).isZero();
    }

    @Test
    void valorDivergenteEhRecusadoSemAlterarPedido() {
        var solicitacao = solicitacaoPublicada(CenarioPagamento.APROVAR);

        assertThatThrownBy(() -> service.registrar(solicitacao.solicitacaoId(), solicitacao.pedidoId(),
                new BigDecimal("1.00"), true, "Aprovado"))
                .isInstanceOf(ResultadoPagamentoInvalidoException.class);

        assertThat(solicitacoes.registros).isZero();
        assertThat(pedidos.porId(solicitacao.pedidoId()).status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
    }

    @Test
    void pedidoDivergenteEhRecusadoSemAlterarPedido() {
        var solicitacao = solicitacaoPublicada(CenarioPagamento.APROVAR);

        assertThatThrownBy(() -> service.registrar(solicitacao.solicitacaoId(), UUID.randomUUID(), VALOR,
                true, "Aprovado"))
                .isInstanceOf(ResultadoPagamentoInvalidoException.class);

        assertThat(solicitacoes.registros).isZero();
        assertThat(pedidos.porId(solicitacao.pedidoId()).status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
    }

    @Test
    void valorComOutraEscalaEhAceito() {
        var solicitacao = solicitacaoPublicada(CenarioPagamento.APROVAR);

        service.registrar(solicitacao.solicitacaoId(), solicitacao.pedidoId(), new BigDecimal("37.8"), true,
                "Aprovado pelo simulador");

        assertThat(pedidos.porId(solicitacao.pedidoId()).status()).isEqualTo(StatusPedido.PAGO);
    }

    private SolicitacaoPagamento solicitacaoPublicada(CenarioPagamento cenario) {
        var pedido = pedidos.salvar(Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")))
                .iniciarPagamento());
        var solicitacao = SolicitacaoPagamento.pendente(UUID.randomUUID(), pedido.id(), pedido.total(), cenario)
                .publicada();
        solicitacoes.solicitacoes.put(solicitacao.solicitacaoId(), solicitacao);
        return solicitacao;
    }

    private static final class PedidosEmMemoria implements Pedidos {

        private final Map<UUID, Pedido> pedidos = new HashMap<>();

        @Override
        public Pedido salvar(Pedido pedido) {
            pedidos.put(pedido.id(), pedido);
            return pedido;
        }

        @Override
        public Optional<Pedido> buscarPorId(UUID id) {
            return Optional.ofNullable(pedidos.get(id));
        }

        private Pedido porId(UUID id) {
            return pedidos.get(id);
        }
    }

    private static final class SolicitacoesEmMemoria implements SolicitacoesPagamento {

        private final PedidosEmMemoria pedidos;
        private final Map<UUID, SolicitacaoPagamento> solicitacoes = new HashMap<>();
        private int registros;

        private SolicitacoesEmMemoria(PedidosEmMemoria pedidos) {
            this.pedidos = pedidos;
        }

        @Override
        public Optional<SolicitacaoPagamento> buscarPorId(UUID solicitacaoId) {
            return Optional.ofNullable(solicitacoes.get(solicitacaoId));
        }

        @Override
        public void registrar(Pedido pedidoEmPagamento, SolicitacaoPagamento solicitacao) {
            throw new UnsupportedOperationException("Registrar resultado nao cria solicitacao");
        }

        @Override
        public SolicitacaoPagamento marcarPublicada(UUID solicitacaoId) {
            throw new UnsupportedOperationException("Registrar resultado nao publica");
        }

        @Override
        public List<SolicitacaoPagamento> listarPendentesDePublicacao() {
            throw new UnsupportedOperationException("Registrar resultado nao lista pendentes");
        }

        @Override
        public void registrarResultado(Pedido pedidoAtualizado, SolicitacaoPagamento solicitacaoResolvida) {
            registros++;
            pedidos.salvar(pedidoAtualizado);
            solicitacoes.put(solicitacaoResolvida.solicitacaoId(), solicitacaoResolvida);
        }

        private SolicitacaoPagamento porId(UUID solicitacaoId) {
            return solicitacoes.get(solicitacaoId);
        }
    }
}
