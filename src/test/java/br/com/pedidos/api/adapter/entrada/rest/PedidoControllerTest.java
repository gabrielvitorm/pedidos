package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.application.port.in.CriarPedido;
import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PedidoControllerTest {

    @Test
    void deveResponderPedidoCriadoComTotalDecimal() throws Exception {
        CriarPedido criarPedido = (cliente, itens) -> Pedido.novo(cliente)
                .adicionarItem(itens.getFirst());
        AdicionarItem adicionarItem = (id, sku, quantidade, precoUnitario) -> {
            throw new UnsupportedOperationException();
        };
        MockMvc mvc = standaloneSetup(new PedidoController(criarPedido, adicionarItem))
                .setControllerAdvice(new PedidoExceptionHandler())
                .build();

        mvc.perform(post("/pedidos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "clienteId":"c-1",
                                  "itens":[{"sku":"CAFE-500","quantidade":2,"precoUnitario":18.90}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value("c-1"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.total").value(37.8));
    }
}
