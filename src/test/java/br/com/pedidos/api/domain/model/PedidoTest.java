package br.com.pedidos.api.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class PedidoTest {

    @Test
    void criaRascunhoAbertoVazioComUuid() {
        var pedido = Pedido.novo("c-1");

        assertThat(pedido.id()).isNotNull();
        assertThat(pedido.cliente()).isEqualTo("c-1");
        assertThat(pedido.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(pedido.itens()).isEmpty();
    }

    @Test
    void adicionaItemSemAlterarPedidoOriginal() {
        var pedido = Pedido.novo("c-1");
        var item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        var atualizado = pedido.adicionarItem(item);

        assertThat(pedido.itens()).isEmpty();
        assertThat(atualizado.id()).isEqualTo(pedido.id());
        assertThat(atualizado.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(atualizado.itens()).containsExactly(item);
    }

    @Test
    void calculaTotalDoExemploDaAula() {
        var pedido = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));

        assertThat(pedido.total()).isEqualByComparingTo("37.80");
    }

    @Test
    void somaTotalDeVariosItens() {
        var pedido = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")))
                .adicionarItem(new ItemPedido("PAO-001", 3, new BigDecimal("2.50")));

        assertThat(pedido.total()).isEqualByComparingTo("45.30");
    }

    @Test
    void naoPermiteAlterarListaExposta() {
        var pedido = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> pedido.itens().add(new ItemPedido("PAO-001", 1, new BigDecimal("2.50"))));
    }

    @Test
    void protegeListaRecebidaNaConstrucao() {
        var itens = new java.util.ArrayList<>(List.of(
                new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"))));
        var pedido = new Pedido(java.util.UUID.randomUUID(), "c-1", StatusPedido.ABERTO, itens);

        itens.clear();

        assertThat(pedido.itens()).hasSize(1);
    }

    @Test
    void pedidoAbertoIniciaPagamentoSemAlterarOriginal() {
        var pedido = pedidoComCafe();

        var emPagamento = pedido.iniciarPagamento();

        assertThat(pedido.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(emPagamento.status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
        assertThat(emPagamento.id()).isEqualTo(pedido.id());
        assertThat(emPagamento.itens()).isEqualTo(pedido.itens());
    }

    @Test
    void pagamentoAprovadoDeixaPedidoPago() {
        var pago = pedidoComCafe().iniciarPagamento().aprovarPagamento();

        assertThat(pago.status()).isEqualTo(StatusPedido.PAGO);
        assertThat(pago.total()).isEqualByComparingTo("37.80");
    }

    @Test
    void pagamentoRecusadoVoltaPedidoParaAbertoComMesmosItens() {
        var pedido = pedidoComCafe();

        var recusado = pedido.iniciarPagamento().recusarPagamento();

        assertThat(recusado.status()).isEqualTo(StatusPedido.ABERTO);
        assertThat(recusado.id()).isEqualTo(pedido.id());
        assertThat(recusado.itens()).isEqualTo(pedido.itens());
    }

    @Test
    void pedidoRecusadoPodeIniciarNovoPagamento() {
        var pedido = pedidoComCafe().iniciarPagamento().recusarPagamento();

        assertThat(pedido.iniciarPagamento().status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
    }

    @Test
    void pedidoAbertoPodeSerCancelado() {
        assertThat(Pedido.novo("c-1").cancelar().status()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void pedidoEmPagamentoNaoAceitaItem() {
        var pedido = pedidoComCafe().iniciarPagamento();

        assertThatIllegalStateException()
                .isThrownBy(() -> pedido.adicionarItem(new ItemPedido("CAFE-500", 1, new BigDecimal("18.90"))));
    }

    @Test
    void pedidoPagoNaoAceitaItem() {
        var pedido = pedidoPago();

        assertThatIllegalStateException()
                .isThrownBy(() -> pedido.adicionarItem(new ItemPedido("CAFE-500", 1, new BigDecimal("18.90"))));
    }

    @Test
    void pedidoCanceladoNaoAceitaItem() {
        var pedido = Pedido.novo("c-1").cancelar();

        assertThatIllegalStateException()
                .isThrownBy(() -> pedido.adicionarItem(new ItemPedido("CAFE-500", 1, new BigDecimal("18.90"))));
    }

    @Test
    void naoPermiteIniciarPagamentoForaDeAberto() {
        assertThatIllegalStateException().isThrownBy(() -> pedidoComCafe().iniciarPagamento().iniciarPagamento());
        assertThatIllegalStateException().isThrownBy(() -> pedidoPago().iniciarPagamento());
        assertThatIllegalStateException().isThrownBy(() -> Pedido.novo("c-1").cancelar().iniciarPagamento());
    }

    @Test
    void naoPermiteAprovarPagamentoForaDeEmPagamento() {
        assertThatIllegalStateException().isThrownBy(() -> pedidoComCafe().aprovarPagamento());
        assertThatIllegalStateException().isThrownBy(() -> pedidoPago().aprovarPagamento());
        assertThatIllegalStateException().isThrownBy(() -> Pedido.novo("c-1").cancelar().aprovarPagamento());
    }

    @Test
    void naoPermiteRecusarPagamentoForaDeEmPagamento() {
        assertThatIllegalStateException().isThrownBy(() -> pedidoComCafe().recusarPagamento());
        assertThatIllegalStateException().isThrownBy(() -> pedidoPago().recusarPagamento());
        assertThatIllegalStateException().isThrownBy(() -> Pedido.novo("c-1").cancelar().recusarPagamento());
    }

    @Test
    void naoPermiteCancelarPedidoEmPagamento() {
        assertThatIllegalStateException().isThrownBy(() -> pedidoComCafe().iniciarPagamento().cancelar());
    }

    @Test
    void naoPermiteCancelarPedidoPago() {
        assertThatIllegalStateException().isThrownBy(() -> pedidoPago().cancelar());
    }

    @Test
    void naoPermiteCancelarPedidoCancelado() {
        assertThatIllegalStateException().isThrownBy(() -> Pedido.novo("c-1").cancelar().cancelar());
    }

    private static Pedido pedidoComCafe() {
        return Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));
    }

    private static Pedido pedidoPago() {
        return pedidoComCafe().iniciarPagamento().aprovarPagamento();
    }
}
