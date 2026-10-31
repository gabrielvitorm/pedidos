package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.application.port.in.ConsultarPedido;
import br.com.pedidos.api.application.port.in.CriarPedido;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PedidoControllerTest {

    private static final Pedido PEDIDO_COM_CAFE = Pedido.novo("c-1")
            .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));

    @Test
    void deveResponderPedidoCriadoComTotalDecimal() throws Exception {
        CriarPedido criarPedido = (cliente, itens) -> Pedido.novo(cliente)
                .adicionarItem(itens.getFirst());

        mvc(criarPedido, naoConsulta()).perform(post("/pedidos")
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

    @Test
    void deveConsultarPedidoExistente() throws Exception {
        ConsultarPedido consultarPedido = id -> PEDIDO_COM_CAFE;

        mvc(naoCria(), consultarPedido).perform(get("/pedidos/{id}", PEDIDO_COM_CAFE.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PEDIDO_COM_CAFE.id().toString()))
                .andExpect(jsonPath("$.clienteId").value("c-1"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.itens[0].sku").value("CAFE-500"))
                .andExpect(jsonPath("$.total").value(37.8));
    }

    @Test
    void deveResponder404ParaPedidoInexistente() throws Exception {
        ConsultarPedido consultarPedido = id -> {
            throw new PedidoNaoEncontradoException(id);
        };

        mvc(naoCria(), consultarPedido).perform(get("/pedidos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    void deveResponder400ParaUuidMalformado() throws Exception {
        mvc(naoCria(), naoConsulta()).perform(get("/pedidos/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Requisição inválida"));
    }

    private static MockMvc mvc(CriarPedido criarPedido, ConsultarPedido consultarPedido) {
        AdicionarItem adicionarItem = (id, sku, quantidade, precoUnitario) -> {
            throw new UnsupportedOperationException();
        };
        return standaloneSetup(new PedidoController(criarPedido, adicionarItem, consultarPedido))
                .setControllerAdvice(new PedidoExceptionHandler())
                .build();
    }

    private static CriarPedido naoCria() {
        return (cliente, itens) -> {
            throw new UnsupportedOperationException();
        };
    }

    private static ConsultarPedido naoConsulta() {
        return id -> {
            throw new UnsupportedOperationException();
        };
    }
}
