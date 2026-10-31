package br.com.pedidos.api.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ItemPedidoTest {

    @Test
    void criaItemComQuantidadeEPrecoPositivos() {
        var item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        assertThat(item.produto()).isEqualTo("CAFE-500");
        assertThat(item.quantidade()).isEqualTo(2);
        assertThat(item.precoUnitario()).isEqualByComparingTo("18.90");
    }

    @Test
    void rejeitaQuantidadeZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ItemPedido("CAFE-500", 0, new BigDecimal("18.90")));
    }

    @Test
    void rejeitaQuantidadeNegativa() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ItemPedido("CAFE-500", -1, new BigDecimal("18.90")));
    }

    @Test
    void rejeitaPrecoZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ItemPedido("CAFE-500", 2, BigDecimal.ZERO));
    }

    @Test
    void rejeitaPrecoNegativo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ItemPedido("CAFE-500", 2, new BigDecimal("-0.01")));
    }
}
