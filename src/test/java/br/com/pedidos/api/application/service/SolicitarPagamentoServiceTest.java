package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PedidoEstadoIncompativelException;
import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.exception.PublicacaoPagamentoException;
import br.com.pedidos.api.application.exception.SolicitacaoConflitanteException;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.PublicadorPagamentos;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import br.com.pedidos.api.domain.model.StatusPedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolicitarPagamentoServiceTest {

    private final PedidosEmMemoria pedidos = new PedidosEmMemoria();
    private final SolicitacoesEmMemoria solicitacoes = new SolicitacoesEmMemoria(pedidos);
    private final PublicadorFalso publicador = new PublicadorFalso();
    private final SolicitarPagamentoService service =
            new SolicitarPagamentoService(pedidos, solicitacoes, publicador);

    @Test
    void solicitaPagamentoComValorDoPedidoEPublica() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();

        var resultado = service.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.APROVAR);

        assertThat(resultado.solicitacaoId()).isEqualTo(solicitacaoId);
        assertThat(resultado.pedidoId()).isEqualTo(pedido.id());
        assertThat(resultado.valor()).isEqualByComparingTo("37.80");
        assertThat(resultado.cenario()).isEqualTo(CenarioPagamento.APROVAR);
        assertThat(resultado.status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
        assertThat(pedidos.porId(pedido.id()).status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
        assertThat(publicador.publicadas).hasSize(1);
        assertThat(solicitacoes.porId(solicitacaoId).status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
    }

    @Test
    void registraAntesDePublicar() {
        var pedido = pedidos.salvar(pedidoComCafe());
        publicador.aoPublicar = solicitacao -> assertThat(solicitacoes.porId(solicitacao.solicitacaoId()))
                .isNotNull();

        service.solicitar(pedido.id(), UUID.randomUUID(), CenarioPagamento.APROVAR);

        assertThat(publicador.publicadas).hasSize(1);
    }

    @Test
    void pedidoInexistenteNaoRegistraNemPublica() {
        assertThatThrownBy(() -> service.solicitar(UUID.randomUUID(), UUID.randomUUID(), CenarioPagamento.APROVAR))
                .isInstanceOf(PedidoNaoEncontradoException.class);

        assertThat(solicitacoes.registros).isZero();
        assertThat(publicador.publicadas).isEmpty();
    }

    @Test
    void pedidoEmPagamentoRecusaNovaSolicitacao() {
        var pedido = pedidos.salvar(pedidoComCafe().iniciarPagamento());

        assertEstadoIncompativelSemEfeito(pedido);
    }

    @Test
    void pedidoPagoRecusaSolicitacao() {
        var pedido = pedidos.salvar(pedidoComCafe().iniciarPagamento().aprovarPagamento());

        assertEstadoIncompativelSemEfeito(pedido);
    }

    @Test
    void pedidoCanceladoRecusaSolicitacao() {
        var pedido = pedidos.salvar(pedidoComCafe().cancelar());

        assertEstadoIncompativelSemEfeito(pedido);
    }

    @Test
    void repeticaoJaPublicadaComMesmoConteudoNaoPublicaNovamente() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();
        var primeira = service.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.APROVAR);

        var repetida = service.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.APROVAR);

        assertThat(repetida).isEqualTo(primeira);
        assertThat(publicador.publicadas).hasSize(1);
        assertThat(solicitacoes.registros).isEqualTo(1);
    }

    @Test
    void repeticaoPendenteRepublicaEMarcaPublicada() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();
        publicador.falhar = true;
        assertThatThrownBy(() -> service.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.APROVAR))
                .isInstanceOf(PublicacaoPagamentoException.class);
        publicador.falhar = false;

        var repetida = service.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.APROVAR);

        assertThat(repetida.status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
        assertThat(publicador.publicadas).hasSize(1);
        assertThat(solicitacoes.registros).isEqualTo(1);
        assertThat(pedidos.porId(pedido.id()).status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
    }

    @Test
    void chaveReutilizadaComOutroCenarioEhConflito() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();
        service.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.APROVAR);

        assertThatThrownBy(() -> service.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.RECUSAR))
                .isInstanceOf(SolicitacaoConflitanteException.class);

        assertThat(publicador.publicadas).hasSize(1);
        assertThat(solicitacoes.porId(solicitacaoId).cenario()).isEqualTo(CenarioPagamento.APROVAR);
    }

    @Test
    void chaveReutilizadaComOutroPedidoEhConflito() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var outroPedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();
        service.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.APROVAR);

        assertThatThrownBy(() -> service.solicitar(outroPedido.id(), solicitacaoId, CenarioPagamento.APROVAR))
                .isInstanceOf(SolicitacaoConflitanteException.class);

        assertThat(pedidos.porId(outroPedido.id()).status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(publicador.publicadas).hasSize(1);
    }

    @Test
    void falhaDePublicacaoMantemSolicitacaoPendenteEPedidoEmPagamento() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();
        publicador.falhar = true;

        assertThatThrownBy(() -> service.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.APROVAR))
                .isInstanceOf(PublicacaoPagamentoException.class);

        assertThat(solicitacoes.porId(solicitacaoId).status())
                .isEqualTo(SolicitacaoPagamento.Status.PENDENTE_PUBLICACAO);
        assertThat(pedidos.porId(pedido.id()).status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
    }

    private void assertEstadoIncompativelSemEfeito(Pedido pedido) {
        var statusAntes = pedido.status();

        assertThatThrownBy(() -> service.solicitar(pedido.id(), UUID.randomUUID(), CenarioPagamento.APROVAR))
                .isInstanceOf(PedidoEstadoIncompativelException.class);

        assertThat(pedidos.porId(pedido.id()).status()).isEqualTo(statusAntes);
        assertThat(solicitacoes.registros).isZero();
        assertThat(publicador.publicadas).isEmpty();
    }

    private static Pedido pedidoComCafe() {
        return Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));
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
            registros++;
            pedidos.salvar(pedidoEmPagamento);
            solicitacoes.put(solicitacao.solicitacaoId(), solicitacao);
        }

        @Override
        public SolicitacaoPagamento marcarPublicada(UUID solicitacaoId) {
            var publicada = solicitacoes.get(solicitacaoId).publicada();
            solicitacoes.put(solicitacaoId, publicada);
            return publicada;
        }

        private SolicitacaoPagamento porId(UUID solicitacaoId) {
            return solicitacoes.get(solicitacaoId);
        }
    }

    private static final class PublicadorFalso implements PublicadorPagamentos {

        private final List<SolicitacaoPagamento> publicadas = new ArrayList<>();
        private boolean falhar;
        private java.util.function.Consumer<SolicitacaoPagamento> aoPublicar = solicitacao -> {
        };

        @Override
        public void publicar(SolicitacaoPagamento solicitacao) {
            if (falhar) {
                throw new PublicacaoPagamentoException("Broker nao confirmou a publicacao");
            }
            aoPublicar.accept(solicitacao);
            publicadas.add(solicitacao);
        }
    }
}
