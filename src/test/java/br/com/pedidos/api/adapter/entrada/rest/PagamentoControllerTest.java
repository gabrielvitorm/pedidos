package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.application.exception.PedidoEstadoIncompativelException;
import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.exception.PublicacaoPagamentoException;
import br.com.pedidos.api.application.exception.SolicitacaoConflitanteException;
import br.com.pedidos.api.application.port.in.SolicitarPagamento;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PagamentoControllerTest {

    private static final UUID PEDIDO = UUID.randomUUID();
    private static final UUID SOLICITACAO = UUID.randomUUID();

    @Test
    void solicitacaoAceitaResponde202ComPedidoEmPagamento() throws Exception {
        SolicitarPagamento solicitarPagamento = (pedidoId, solicitacaoId, cenario) ->
                SolicitacaoPagamento.pendente(solicitacaoId, pedidoId, new BigDecimal("37.80"), cenario).publicada();

        solicitar(solicitarPagamento, corpo(SOLICITACAO.toString(), "APROVAR"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.solicitacaoId").value(SOLICITACAO.toString()))
                .andExpect(jsonPath("$.pedidoId").value(PEDIDO.toString()))
                .andExpect(jsonPath("$.status").value("EM_PAGAMENTO"))
                .andExpect(jsonPath("$.aprovado").doesNotExist());
    }

    @Test
    void jsonMalformadoResponde400() throws Exception {
        solicitar(naoChamado(), "{\"solicitacaoId\":")
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitacaoIdInvalidoResponde400() throws Exception {
        solicitar(naoChamado(), corpo("abc", "APROVAR"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solicitacaoIdAusenteResponde400() throws Exception {
        solicitar(naoChamado(), "{\"cenario\":\"APROVAR\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void cenarioDesconhecidoResponde400() throws Exception {
        solicitar(naoChamado(), corpo(SOLICITACAO.toString(), "TALVEZ"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void valorEnviadoPeloClienteResponde400() throws Exception {
        solicitar(naoChamado(), """
                {"solicitacaoId":"%s","cenario":"APROVAR","valor":1.00}
                """.formatted(SOLICITACAO))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pedidoInexistenteResponde404() throws Exception {
        solicitar(falhaCom(new PedidoNaoEncontradoException(PEDIDO)), corpo(SOLICITACAO.toString(), "APROVAR"))
                .andExpect(status().isNotFound());
    }

    @Test
    void estadoIncompativelResponde409() throws Exception {
        solicitar(falhaCom(new PedidoEstadoIncompativelException("Pedido deve estar aberto", null)),
                corpo(SOLICITACAO.toString(), "APROVAR"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    void chaveReutilizadaComOutroConteudoResponde409() throws Exception {
        solicitar(falhaCom(new SolicitacaoConflitanteException(SOLICITACAO)), corpo(SOLICITACAO.toString(), "RECUSAR"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    void falhaDePublicacaoResponde503() throws Exception {
        solicitar(falhaCom(new PublicacaoPagamentoException("Broker indisponivel")),
                corpo(SOLICITACAO.toString(), "APROVAR"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    private static ResultActions solicitar(SolicitarPagamento solicitarPagamento, String corpo) throws Exception {
        MockMvc mvc = standaloneSetup(new PagamentoController(solicitarPagamento))
                .setControllerAdvice(new PedidoExceptionHandler())
                .build();
        return mvc.perform(post("/pedidos/{pedidoId}/pagamentos", PEDIDO)
                .contentType("application/json")
                .content(corpo));
    }

    private static String corpo(String solicitacaoId, String cenario) {
        return """
                {"solicitacaoId":"%s","cenario":"%s"}
                """.formatted(solicitacaoId, cenario);
    }

    private static SolicitarPagamento falhaCom(RuntimeException exception) {
        return (pedidoId, solicitacaoId, cenario) -> {
            throw exception;
        };
    }

    private static SolicitarPagamento naoChamado() {
        return falhaCom(new UnsupportedOperationException("Caso de uso nao deve ser chamado"));
    }
}
