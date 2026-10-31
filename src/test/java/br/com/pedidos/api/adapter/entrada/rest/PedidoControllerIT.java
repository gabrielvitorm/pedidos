package br.com.pedidos.api.adapter.entrada.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveCriarPedidoECalcularTotal() throws Exception {
        var pedidosAntes = jdbc.queryForObject("SELECT COUNT(*) FROM pedido WHERE cliente = 'c-http'", Integer.class);

        mvc.perform(post("/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId":"c-http",
                                  "itens":[{"sku":"CAFE-500","quantidade":2,"precoUnitario":18.90}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.clienteId").value("c-http"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.total").value(37.8))
                .andExpect(jsonPath("$.itens[0].sku").value("CAFE-500"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(18.9));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pedido WHERE cliente = 'c-http'", Integer.class))
                .isEqualTo(pedidosAntes + 1);
    }

    @Test
    void quantidadeZeroDeveRetornar422SemPersistir() throws Exception {
        var antes = totais();

        mvc.perform(post("/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"clienteId":"c-http-zero","itens":[{"sku":"CAFE-500","quantidade":0,"precoUnitario":18.90}]}
                                """))
                .andExpect(status().isUnprocessableContent());

        assertThat(totais()).isEqualTo(antes);
    }

    @Test
    void itensVaziosDevemRetornar422SemPersistir() throws Exception {
        var antes = totais();

        mvc.perform(post("/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{\"clienteId\":\"c-http-vazio\",\"itens\":[]}"))
                .andExpect(status().isUnprocessableContent());

        assertThat(totais()).isEqualTo(antes);
    }

    @Test
    void jsonMalformadoDeveRetornar400SemPersistir() throws Exception {
        var antes = totais();

        mvc.perform(post("/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{\"clienteId\":\"c-http-malformado\",\"itens\":["))
                .andExpect(status().isBadRequest());

        assertThat(totais()).isEqualTo(antes);
    }

    @Test
    void campoObrigatorioAusenteDeveRetornar400() throws Exception {
        mvc.perform(post("/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("{\"itens\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveConsultarPedidoCriadoPorGet() throws Exception {
        var criado = mvc.perform(post("/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"clienteId":"c-http-get","itens":[{"sku":"CAFE-500","quantidade":2,"precoUnitario":18.90}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var id = JsonPath.<String>read(criado, "$.id");
        var antes = totais();

        mvc.perform(get("/pedidos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.clienteId").value("c-http-get"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.itens[0].sku").value("CAFE-500"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(18.9))
                .andExpect(jsonPath("$.total").value(37.8));

        assertThat(totais()).isEqualTo(antes);
    }

    @Test
    void consultaDePedidoInexistenteDeveRetornar404() throws Exception {
        mvc.perform(get("/pedidos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    void consultaComUuidMalformadoDeveRetornar400() throws Exception {
        mvc.perform(get("/pedidos/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Requisição inválida"));
    }

    private Totais totais() {
        return new Totais(
                jdbc.queryForObject("SELECT COUNT(*) FROM pedido", Integer.class),
                jdbc.queryForObject("SELECT COUNT(*) FROM item_pedido", Integer.class));
    }

    private record Totais(int pedidos, int itens) {
    }
}
