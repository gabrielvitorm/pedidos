package br.com.pedidos.api.adapter.out.persistence;

import br.com.pedidos.api.application.exception.PedidoEstadoIncompativelException;
import br.com.pedidos.api.application.exception.ResultadoPagamentoInvalidoException;
import br.com.pedidos.api.application.exception.SolicitacaoConflitanteException;
import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import br.com.pedidos.api.domain.model.StatusPedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SolicitacoesPagamentoJpaAdapterIT {

    @Autowired
    private SolicitacoesPagamentoJpaAdapter adapter;

    @Autowired
    private PedidosJpaAdapter pedidos;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registraPedidoEmPagamentoESolicitacaoPendente() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacao = pendente(UUID.randomUUID(), pedido, CenarioPagamento.APROVAR);

        adapter.registrar(pedido.iniciarPagamento(), solicitacao);

        assertThat(pedidos.buscarPorId(pedido.id()).orElseThrow().status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
        var recuperada = adapter.buscarPorId(solicitacao.solicitacaoId()).orElseThrow();
        assertThat(recuperada.solicitacaoId()).isEqualTo(solicitacao.solicitacaoId());
        assertThat(recuperada.pedidoId()).isEqualTo(pedido.id());
        assertThat(recuperada.valor()).isEqualByComparingTo("37.80");
        assertThat(recuperada.cenario()).isEqualTo(CenarioPagamento.APROVAR);
        assertThat(recuperada.status()).isEqualTo(SolicitacaoPagamento.Status.PENDENTE_PUBLICACAO);
    }

    @Test
    void marcaSolicitacaoComoPublicada() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacao = pendente(UUID.randomUUID(), pedido, CenarioPagamento.RECUSAR);
        adapter.registrar(pedido.iniciarPagamento(), solicitacao);

        var publicada = adapter.marcarPublicada(solicitacao.solicitacaoId());

        assertThat(publicada.status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
        assertThat(adapter.buscarPorId(solicitacao.solicitacaoId()).orElseThrow().status())
                .isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
    }

    @Test
    void pedidoQueNaoEstaAbertoNoBancoNaoRegistraSolicitacao() {
        var pedido = pedidos.salvar(pedidoComCafe().iniciarPagamento());
        var solicitacaoId = UUID.randomUUID();

        assertThatThrownBy(() -> adapter.registrar(pedido,
                pendente(solicitacaoId, pedido, CenarioPagamento.APROVAR)))
                .isInstanceOf(PedidoEstadoIncompativelException.class);

        assertThat(linhasDaSolicitacao(solicitacaoId)).isZero();
        assertThat(pedidos.buscarPorId(pedido.id()).orElseThrow().status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
    }

    @Test
    void chaveDuplicadaEhConflitoEDesfazStatusDoSegundoPedido() {
        var primeiro = pedidos.salvar(pedidoComCafe());
        var segundo = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();
        adapter.registrar(primeiro.iniciarPagamento(), pendente(solicitacaoId, primeiro, CenarioPagamento.APROVAR));

        assertThatThrownBy(() -> adapter.registrar(segundo.iniciarPagamento(),
                pendente(solicitacaoId, segundo, CenarioPagamento.RECUSAR)))
                .isInstanceOf(SolicitacaoConflitanteException.class);

        var original = adapter.buscarPorId(solicitacaoId).orElseThrow();
        assertThat(original.pedidoId()).isEqualTo(primeiro.id());
        assertThat(original.cenario()).isEqualTo(CenarioPagamento.APROVAR);
        assertThat(linhasDaSolicitacao(solicitacaoId)).isEqualTo(1);
        assertThat(pedidos.buscarPorId(segundo.id()).orElseThrow().status()).isEqualTo(StatusPedido.ABERTO);
    }

    @Test
    void resultadoGravadoAntesDaPublicacaoNaoEhDesfeitoAoMarcarPublicada() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacao = pendente(UUID.randomUUID(), pedido, CenarioPagamento.APROVAR);
        var emPagamento = pedido.iniciarPagamento();
        adapter.registrar(emPagamento, solicitacao);

        adapter.registrarResultado(emPagamento.aprovarPagamento(), solicitacao.resolver(true, "Aprovado"));
        adapter.marcarPublicada(solicitacao.solicitacaoId());

        var gravada = adapter.buscarPorId(solicitacao.solicitacaoId()).orElseThrow();
        assertThat(gravada.status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
        assertThat(gravada.aprovado()).isTrue();
        assertThat(gravada.motivo()).isEqualTo("Aprovado");
        assertThat(pedidos.buscarPorId(pedido.id()).orElseThrow().status()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    void publicacaoMarcadaAntesDoResultadoNaoEhDesfeitaAoGravarResultado() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacao = pendente(UUID.randomUUID(), pedido, CenarioPagamento.RECUSAR);
        var emPagamento = pedido.iniciarPagamento();
        adapter.registrar(emPagamento, solicitacao);

        adapter.marcarPublicada(solicitacao.solicitacaoId());
        adapter.registrarResultado(emPagamento.recusarPagamento(), solicitacao.resolver(false, "Recusado"));

        var gravada = adapter.buscarPorId(solicitacao.solicitacaoId()).orElseThrow();
        assertThat(gravada.status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
        assertThat(gravada.aprovado()).isFalse();
        assertThat(pedidos.buscarPorId(pedido.id()).orElseThrow().status()).isEqualTo(StatusPedido.ABERTO);
    }

    @Test
    void segundoResultadoEhRecusadoEDesfazStatusDoPedido() {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacao = pendente(UUID.randomUUID(), pedido, CenarioPagamento.APROVAR);
        var emPagamento = pedido.iniciarPagamento();
        adapter.registrar(emPagamento, solicitacao);
        adapter.registrarResultado(emPagamento.recusarPagamento(), solicitacao.resolver(false, "Recusado"));
        var novaTentativa = pedidos.buscarPorId(pedido.id()).orElseThrow().iniciarPagamento();
        jdbc.update("UPDATE pedido SET status = 'EM_PAGAMENTO' WHERE id = ?", pedido.id());

        assertThatThrownBy(() -> adapter.registrarResultado(novaTentativa.aprovarPagamento(),
                solicitacao.resolver(true, "Aprovado")))
                .isInstanceOf(ResultadoPagamentoInvalidoException.class);

        assertThat(pedidos.buscarPorId(pedido.id()).orElseThrow().status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
        assertThat(adapter.buscarPorId(solicitacao.solicitacaoId()).orElseThrow().aprovado()).isFalse();
    }

    @Test
    void listaSomentePendentesSemResultado() {
        var pendente = registrarPendente(CenarioPagamento.APROVAR);
        var publicada = registrarPendente(CenarioPagamento.APROVAR);
        adapter.marcarPublicada(publicada.solicitacaoId());
        var resolvidaSemPublicar = registrarPendente(CenarioPagamento.RECUSAR);
        var pedidoResolvido = pedidos.buscarPorId(resolvidaSemPublicar.pedidoId()).orElseThrow();
        adapter.registrarResultado(pedidoResolvido.recusarPagamento(), resolvidaSemPublicar.resolver(false, "Recusado"));

        var ids = adapter.listarPendentesDePublicacao().stream().map(SolicitacaoPagamento::solicitacaoId).toList();

        assertThat(ids).contains(pendente.solicitacaoId());
        assertThat(ids).doesNotContain(publicada.solicitacaoId(), resolvidaSemPublicar.solicitacaoId());
    }

    private SolicitacaoPagamento registrarPendente(CenarioPagamento cenario) {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacao = pendente(UUID.randomUUID(), pedido, cenario);
        adapter.registrar(pedido.iniciarPagamento(), solicitacao);
        return solicitacao;
    }

    @Test
    void tabelaContemSomenteColunasDaSpec() {
        var colunas = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'solicitacao_pagamento'",
                String.class);

        assertThat(colunas).containsExactlyInAnyOrder(
                "solicitacao_id", "pedido_id", "valor", "cenario", "status", "aprovado", "motivo");
    }

    private int linhasDaSolicitacao(UUID solicitacaoId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM solicitacao_pagamento WHERE solicitacao_id = ?", Integer.class, solicitacaoId);
    }

    private static SolicitacaoPagamento pendente(UUID solicitacaoId, Pedido pedido, CenarioPagamento cenario) {
        return SolicitacaoPagamento.pendente(solicitacaoId, pedido.id(), pedido.total(), cenario);
    }

    private static Pedido pedidoComCafe() {
        return Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));
    }
}
