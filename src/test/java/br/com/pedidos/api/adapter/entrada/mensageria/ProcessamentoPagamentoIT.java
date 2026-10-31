package br.com.pedidos.api.adapter.entrada.mensageria;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo contra PostgreSQL e RabbitMQ locais: HTTP, publicação, simulador, resultado e filas de erro.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProcessamentoPagamentoIT {

    private static final String PAINEL = "http://127.0.0.1:15672/api";
    private static final String CREDENCIAL_LOCAL = "aula:aula-local";
    private static final long ESPERA_MS = 15_000;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private JsonMapper jsonMapper;

    // O painel encerra a conexão quando o cliente tenta upgrade para HTTP/2 em requisições com corpo
    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @Test
    void aprovacaoLevaPedidoAPago() throws Exception {
        var pedidoId = criarPedido();

        solicitarPagamento(pedidoId, UUID.randomUUID(), "APROVAR");

        assertThat(aguardarStatus(pedidoId, "PAGO")).isEqualTo("PAGO");
    }

    @Test
    void recusaVoltaPedidoParaAbertoComMesmosItens() throws Exception {
        var pedidoId = criarPedido();

        // O 202 garante que o pedido já passou para EM_PAGAMENTO; voltar a ABERTO prova a recusa aplicada
        solicitarPagamento(pedidoId, UUID.randomUUID(), "RECUSAR");

        aguardarStatus(pedidoId, "ABERTO");
        var pedido = consultarPedido(pedidoId);
        assertThat(JsonPath.<Integer>read(pedido, "$.itens.length()")).isEqualTo(1);
        assertThat(JsonPath.<Double>read(pedido, "$.total")).isEqualTo(37.8);
    }

    @Test
    void resultadoDuplicadoNaoAlteraPedidoNemVaiParaFilaDeErro() throws Exception {
        var pedidoId = criarPedido();
        var solicitacaoId = UUID.randomUUID();
        solicitarPagamento(pedidoId, solicitacaoId, "APROVAR");
        aguardarStatus(pedidoId, "PAGO");

        publicarResultado(resultado(solicitacaoId, pedidoId, "37.80", true));
        aguardarFilaConsumida("pedidos.resultados");

        assertThat(statusDoPedido(pedidoId)).isEqualTo("PAGO");
        assertThat(apareceNaFila("pedidos.resultados.erros", solicitacaoId.toString(), 3_000)).isFalse();
    }

    @Test
    void resultadoConflitanteVaiParaFilaDeErroSemAlterarPedido() throws Exception {
        var pedidoId = criarPedido();
        var solicitacaoId = UUID.randomUUID();
        solicitarPagamento(pedidoId, solicitacaoId, "APROVAR");
        aguardarStatus(pedidoId, "PAGO");

        publicarResultado(resultado(solicitacaoId, pedidoId, "37.80", false));

        assertThat(apareceNaFila("pedidos.resultados.erros", solicitacaoId.toString(), ESPERA_MS)).isTrue();
        assertThat(statusDoPedido(pedidoId)).isEqualTo("PAGO");
    }

    @Test
    void resultadoDeSolicitacaoDesconhecidaVaiParaFilaDeErro() throws Exception {
        var solicitacaoId = UUID.randomUUID();

        publicarResultado(resultado(solicitacaoId, UUID.randomUUID(), "37.80", true));

        assertThat(apareceNaFila("pedidos.resultados.erros", solicitacaoId.toString(), ESPERA_MS)).isTrue();
    }

    @Test
    void resultadoIlegivelVaiParaFilaDeErro() throws Exception {
        var marcador = "resultado-ilegivel-" + UUID.randomUUID();

        publicar("pagamentos.eventos", "pagamento.resultado", marcador);

        assertThat(apareceNaFila("pedidos.resultados.erros", marcador, ESPERA_MS)).isTrue();
    }

    @Test
    void solicitacaoIlegivelVaiParaFilaDeErroDoSimulador() throws Exception {
        var marcador = "solicitacao-ilegivel-" + UUID.randomUUID();

        publicar("pagamentos.comandos", "pagamento.solicitar", marcador);

        assertThat(apareceNaFila("pagamentos.solicitacoes.erros", marcador, ESPERA_MS)).isTrue();
    }

    @Test
    void solicitacaoComVersaoDesconhecidaVaiParaFilaDeErroDoSimulador() throws Exception {
        var solicitacaoId = UUID.randomUUID();

        publicar("pagamentos.comandos", "pagamento.solicitar", """
                {"versao":2,"solicitacaoId":"%s","pedidoId":"%s","valor":37.80,"cenario":"APROVAR"}
                """.formatted(solicitacaoId, UUID.randomUUID()));

        assertThat(apareceNaFila("pagamentos.solicitacoes.erros", solicitacaoId.toString(), ESPERA_MS)).isTrue();
    }

    private UUID criarPedido() throws Exception {
        var resposta = mvc.perform(post("/pedidos")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"clienteId":"c-processamento","itens":[{"sku":"CAFE-500","quantidade":2,"precoUnitario":18.90}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(resposta, "$.id"));
    }

    private void solicitarPagamento(UUID pedidoId, UUID solicitacaoId, String cenario) throws Exception {
        mvc.perform(post("/pedidos/{pedidoId}/pagamentos", pedidoId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"solicitacaoId":"%s","cenario":"%s"}
                                """.formatted(solicitacaoId, cenario)))
                .andExpect(status().isAccepted());
    }

    private String consultarPedido(UUID pedidoId) throws Exception {
        return mvc.perform(get("/pedidos/{id}", pedidoId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String statusDoPedido(UUID pedidoId) throws Exception {
        return JsonPath.read(consultarPedido(pedidoId), "$.status");
    }

    private String aguardarStatus(UUID pedidoId, String esperado) throws Exception {
        var limite = System.currentTimeMillis() + ESPERA_MS;
        var atual = statusDoPedido(pedidoId);
        while (!esperado.equals(atual) && System.currentTimeMillis() < limite) {
            Thread.sleep(200);
            atual = statusDoPedido(pedidoId);
        }
        assertThat(atual).as("status do pedido %s", pedidoId).isEqualTo(esperado);
        return atual;
    }

    private void aguardarFilaConsumida(String fila) throws Exception {
        var limite = System.currentTimeMillis() + ESPERA_MS;
        while (amqpAdmin.getQueueInfo(fila).getMessageCount() > 0 && System.currentTimeMillis() < limite) {
            Thread.sleep(200);
        }
        assertThat(amqpAdmin.getQueueInfo(fila).getMessageCount()).isZero();
    }

    private String resultado(UUID solicitacaoId, UUID pedidoId, String valor, boolean aprovado) {
        return """
                {"versao":1,"solicitacaoId":"%s","pedidoId":"%s","valor":%s,"aprovado":%s,"motivo":"teste"}
                """.formatted(solicitacaoId, pedidoId, valor, aprovado);
    }

    private void publicarResultado(String corpo) {
        publicar("pagamentos.eventos", "pagamento.resultado", corpo);
    }

    private void publicar(String exchange, String chave, String corpo) {
        rabbitTemplate.send(exchange, chave, MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build());
    }

    /**
     * Procura o marcador nas mensagens da fila pelo painel, devolvendo-as à fila (ack_requeue_true).
     */
    private boolean apareceNaFila(String fila, String marcador, long esperaMs) throws Exception {
        var limite = System.currentTimeMillis() + esperaMs;
        do {
            var autorizacao = Base64.getEncoder().encodeToString(CREDENCIAL_LOCAL.getBytes(StandardCharsets.UTF_8));
            var resposta = http.send(HttpRequest.newBuilder(URI.create(PAINEL + "/queues/%2F/" + fila + "/get"))
                            .header("Authorization", "Basic " + autorizacao)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    "{\"count\":10000,\"ackmode\":\"ack_requeue_true\",\"encoding\":\"auto\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(resposta.statusCode()).isEqualTo(200);
            for (var mensagem : jsonMapper.readTree(resposta.body())) {
                if (mensagem.get("payload").asString().contains(marcador)) {
                    return true;
                }
            }
            Thread.sleep(300);
        } while (System.currentTimeMillis() < limite);
        return false;
    }
}
