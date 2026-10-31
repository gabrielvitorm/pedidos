package br.com.pedidos.api.adapter.entrada.inicializacao;

import br.com.pedidos.api.ApiApplication;
import br.com.pedidos.api.adapter.out.persistence.PedidosJpaAdapter;
import br.com.pedidos.api.adapter.out.persistence.SolicitacoesPagamentoJpaAdapter;
import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import br.com.pedidos.api.domain.model.StatusPedido;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simula a queda do processo entre gravar a solicitação e publicá-la, e o reinício da aplicação.
 * Usa duas instâncias reais da aplicação contra PostgreSQL e RabbitMQ locais.
 */
class RepublicacaoNaInicializacaoIT {

    private static final long ESPERA_MS = 15_000;

    @Test
    void reinicioRepublicaPendenteEPagamentoEhConcluido() throws Exception {
        UUID pedidoId;
        UUID solicitacaoId = UUID.randomUUID();

        try (var antesDaQueda = iniciarAplicacao()) {
            var pedidos = antesDaQueda.getBean(PedidosJpaAdapter.class);
            var solicitacoes = antesDaQueda.getBean(SolicitacoesPagamentoJpaAdapter.class);
            var pedido = pedidos.salvar(Pedido.novo("c-reinicio")
                    .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"))));
            pedidoId = pedido.id();
            // Grava como o caso de uso faria, mas o processo "cai" antes de publicar
            solicitacoes.registrar(pedido.iniciarPagamento(), SolicitacaoPagamento.pendente(
                    solicitacaoId, pedido.id(), pedido.total(), CenarioPagamento.APROVAR));

            assertThat(solicitacoes.buscarPorId(solicitacaoId).orElseThrow().status())
                    .isEqualTo(SolicitacaoPagamento.Status.PENDENTE_PUBLICACAO);
        }

        try (var aposReinicio = iniciarAplicacao()) {
            var pedidos = aposReinicio.getBean(PedidosJpaAdapter.class);
            var solicitacoes = aposReinicio.getBean(SolicitacoesPagamentoJpaAdapter.class);

            assertThat(aguardarStatus(pedidos, pedidoId, StatusPedido.PAGO)).isEqualTo(StatusPedido.PAGO);
            var solicitacao = solicitacoes.buscarPorId(solicitacaoId).orElseThrow();
            assertThat(solicitacao.status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
            assertThat(solicitacao.aprovado()).isTrue();
        }
    }

    private static ConfigurableApplicationContext iniciarAplicacao() {
        return new SpringApplicationBuilder(ApiApplication.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    private static StatusPedido aguardarStatus(PedidosJpaAdapter pedidos, UUID pedidoId, StatusPedido esperado)
            throws InterruptedException {
        var limite = System.currentTimeMillis() + ESPERA_MS;
        var atual = pedidos.buscarPorId(pedidoId).orElseThrow().status();
        while (atual != esperado && System.currentTimeMillis() < limite) {
            Thread.sleep(200);
            atual = pedidos.buscarPorId(pedidoId).orElseThrow().status();
        }
        return atual;
    }
}
