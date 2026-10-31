package br.com.pedidos.api.adapter.out.persistence;

import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PedidosJpaAdapterIT {

    @Autowired
    private PedidosJpaAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void salvaERecuperaPedidoEmNovaTransacaoSemColunaTotal() {
        var pedido = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));

        var salvo = adapter.salvar(pedido);
        var recuperado = adapter.buscarPorId(salvo.id()).orElseThrow();
        var colunasTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'pedido' AND column_name = 'total'",
                Integer.class);

        assertThat(recuperado.id()).isEqualTo(pedido.id());
        assertThat(recuperado.cliente()).isEqualTo("c-1");
        assertThat(recuperado.status()).isEqualTo(br.com.pedidos.api.domain.model.StatusPedido.ABERTO);
        assertThat(recuperado.itens()).hasSize(1);
        assertThat(recuperado.itens().getFirst().produto()).isEqualTo("CAFE-500");
        assertThat(recuperado.itens().getFirst().quantidade()).isEqualTo(2);
        assertThat(recuperado.itens().getFirst().precoUnitario()).isEqualByComparingTo("18.90");
        assertThat(recuperado.total()).isEqualByComparingTo("37.80");
        assertThat(colunasTotal).isZero();
    }
}
