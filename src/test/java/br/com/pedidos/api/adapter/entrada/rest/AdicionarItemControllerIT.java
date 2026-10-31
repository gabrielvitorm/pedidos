package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.adapter.out.persistence.PedidosJpaAdapter;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdicionarItemControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PedidosJpaAdapter pedidos;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void adicionaItemMantemUuidEAtualizaTotal() throws Exception {
        var pedido = salvarPedidoAberto();

        mvc.perform(post("/pedidos/{id}/itens", pedido.id())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"CAFE-500\",\"quantidade\":1,\"precoUnitario\":18.90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedido.id().toString()))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.total").value(56.7));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM item_pedido WHERE pedido_id = ?", Integer.class, pedido.id()))
                .isEqualTo(2);
    }

    @Test
    void pedidoInexistenteRetorna404SemGravar() throws Exception {
        var antes = totalDeLinhas();

        mvc.perform(post("/pedidos/{id}/itens", UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"CAFE-500\",\"quantidade\":1,\"precoUnitario\":18.90}"))
                .andExpect(status().isNotFound());

        assertThat(totalDeLinhas()).isEqualTo(antes);
    }

    @Test
    void pedidoPagoRetorna409SemGravar() throws Exception {
        var pedido = pedidos.salvar(salvarPedidoAberto().iniciarPagamento().aprovarPagamento());
        var antes = totalDeItens(pedido.id());

        mvc.perform(post("/pedidos/{id}/itens", pedido.id())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"CAFE-500\",\"quantidade\":1,\"precoUnitario\":18.90}"))
                .andExpect(status().isConflict());

        assertThat(totalDeItens(pedido.id())).isEqualTo(antes);
    }

    @Test
    void quantidadeInvalidaRetorna422SemGravar() throws Exception {
        var pedido = salvarPedidoAberto();
        var antes = totalDeItens(pedido.id());

        mvc.perform(post("/pedidos/{id}/itens", pedido.id())
                        .contentType(APPLICATION_JSON)
                        .content("{\"sku\":\"CAFE-500\",\"quantidade\":0,\"precoUnitario\":18.90}"))
                .andExpect(status().isUnprocessableContent());

        assertThat(totalDeItens(pedido.id())).isEqualTo(antes);
    }

    private Pedido salvarPedidoAberto() {
        return pedidos.salvar(Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"))));
    }

    private int totalDeItens(UUID id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM item_pedido WHERE pedido_id = ?", Integer.class, id);
    }

    private Totais totalDeLinhas() {
        return new Totais(
                jdbc.queryForObject("SELECT COUNT(*) FROM pedido", Integer.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM item_pedido", Integer.class));
    }

    private record Totais(int pedidos, int itens) {
    }
}
