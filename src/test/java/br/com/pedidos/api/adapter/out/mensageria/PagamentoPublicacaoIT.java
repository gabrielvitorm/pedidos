package br.com.pedidos.api.adapter.out.mensageria;

import br.com.pedidos.api.adapter.out.persistence.PedidosJpaAdapter;
import br.com.pedidos.api.adapter.out.persistence.SolicitacoesPagamentoJpaAdapter;
import br.com.pedidos.api.application.exception.PublicacaoPagamentoException;
import br.com.pedidos.api.application.service.SolicitarPagamentoService;
import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import br.com.pedidos.api.domain.model.StatusPedido;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static br.com.pedidos.api.adapter.out.mensageria.TopologiaPagamentosConfig.CHAVE_SOLICITAR;
import static br.com.pedidos.api.adapter.out.mensageria.TopologiaPagamentosConfig.EXCHANGE_COMANDOS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Executa contra o PostgreSQL e o RabbitMQ locais do Compose.
 * Como o simulador consome pagamentos.solicitacoes, cada teste lê as mensagens publicadas por uma
 * fila espiã temporária ligada ao mesmo exchange e chave, que recebe uma cópia de cada mensagem.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PagamentoPublicacaoIT {

    private static final String PAINEL = "http://127.0.0.1:15672/api";
    private static final String CREDENCIAL_LOCAL = "aula:aula-local";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PedidosJpaAdapter pedidos;

    @Autowired
    private SolicitacoesPagamentoJpaAdapter solicitacoes;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private JsonMapper jsonMapper;

    // O painel encerra a conexão quando o cliente tenta upgrade para HTTP/2 em requisições com corpo
    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    private String filaEspia;

    @BeforeEach
    void criarFilaEspia() {
        // Não usa auto-delete: o receive do RabbitTemplate cancela o consumidor e apagaria a fila
        var fila = QueueBuilder.nonDurable("teste.espia." + UUID.randomUUID()).expires(60_000).build();
        amqpAdmin.declareQueue(fila);
        amqpAdmin.declareBinding(BindingBuilder.bind(fila).to(new DirectExchange(EXCHANGE_COMANDOS))
                .with(CHAVE_SOLICITAR));
        filaEspia = fila.getName();
    }

    @AfterEach
    void removerFilaEspia() {
        amqpAdmin.deleteQueue(filaEspia);
    }

    @Test
    void solicitacaoAceitaPublicaMensagemV1PersistenteEResponde202() throws Exception {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();

        solicitarPorHttp(pedido.id(), solicitacaoId, "APROVAR")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.solicitacaoId").value(solicitacaoId.toString()))
                .andExpect(jsonPath("$.status").value("EM_PAGAMENTO"));

        var mensagens = mensagensDaSolicitacao(solicitacaoId);
        assertThat(mensagens).hasSize(1);
        var propriedades = mensagens.getFirst().getMessageProperties();
        assertThat(propriedades.getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(propriedades.getContentType()).isEqualTo("application/json");
        var corpo = jsonMapper.readTree(mensagens.getFirst().getBody());
        assertThat(corpo.get("versao").asInt()).isEqualTo(1);
        assertThat(corpo.get("pedidoId").asString()).isEqualTo(pedido.id().toString());
        assertThat(corpo.get("cenario").asString()).isEqualTo("APROVAR");
        assertThat(corpo.get("valor").isNumber()).isTrue();
        assertThat(corpo.get("valor").decimalValue()).isEqualByComparingTo("37.80");
        assertThat(solicitacoes.buscarPorId(solicitacaoId).orElseThrow().status())
                .isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
        // O simulador pode já ter processado a solicitação
        assertThat(pedidos.buscarPorId(pedido.id()).orElseThrow().status())
                .isIn(StatusPedido.EM_PAGAMENTO, StatusPedido.PAGO);
    }

    @Test
    void repeticaoComMesmaChaveResponde202SemNovaMensagem() throws Exception {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();
        solicitarPorHttp(pedido.id(), solicitacaoId, "RECUSAR").andExpect(status().isAccepted());

        solicitarPorHttp(pedido.id(), solicitacaoId, "RECUSAR").andExpect(status().isAccepted());

        assertThat(mensagensDaSolicitacao(solicitacaoId)).hasSize(1);
    }

    @Test
    void mesmaChaveComOutroCenarioResponde409() throws Exception {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();
        solicitarPorHttp(pedido.id(), solicitacaoId, "APROVAR").andExpect(status().isAccepted());

        solicitarPorHttp(pedido.id(), solicitacaoId, "RECUSAR").andExpect(status().isConflict());

        assertThat(mensagensDaSolicitacao(solicitacaoId)).hasSize(1);
    }

    @Test
    void pedidoForaDeAbertoRecusaNovaChaveCom409() throws Exception {
        var pedido = pedidos.salvar(pedidoComCafe());
        solicitarPorHttp(pedido.id(), UUID.randomUUID(), "APROVAR").andExpect(status().isAccepted());

        // EM_PAGAMENTO ou já PAGO pelo simulador: ambos recusam nova solicitação
        solicitarPorHttp(pedido.id(), UUID.randomUUID(), "APROVAR").andExpect(status().isConflict());
    }

    @Test
    void pedidoInexistenteResponde404() throws Exception {
        solicitarPorHttp(UUID.randomUUID(), UUID.randomUUID(), "APROVAR").andExpect(status().isNotFound());
    }

    @Test
    void mensagemNaoRoteadaFalhaMesmoComConfirmacaoDoBroker() {
        var publicadorSemBinding = new PublicadorPagamentosRabbit(rabbitTemplate, jsonMapper,
                EXCHANGE_COMANDOS, "chave.sem.binding", Duration.ofSeconds(5));
        var solicitacao = SolicitacaoPagamento.pendente(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("37.80"), CenarioPagamento.APROVAR);

        assertThatThrownBy(() -> publicadorSemBinding.publicar(solicitacao))
                .isInstanceOf(PublicacaoPagamentoException.class)
                .hasMessageContaining("nao roteada");
    }

    @Test
    void brokerIndisponivelMantemPendenteERepeticaoRecupera() throws Exception {
        var pedido = pedidos.salvar(pedidoComCafe());
        var solicitacaoId = UUID.randomUUID();
        var conexaoQuebrada = new CachingConnectionFactory("127.0.0.1", 1);
        conexaoQuebrada.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        var templateQuebrado = new RabbitTemplate(conexaoQuebrada);
        templateQuebrado.setMandatory(true);
        var servicoSemBroker = new SolicitarPagamentoService(pedidos, solicitacoes,
                new PublicadorPagamentosRabbit(templateQuebrado, jsonMapper, EXCHANGE_COMANDOS, CHAVE_SOLICITAR,
                        Duration.ofSeconds(2)));

        try {
            assertThatThrownBy(() -> servicoSemBroker.solicitar(pedido.id(), solicitacaoId, CenarioPagamento.APROVAR))
                    .isInstanceOf(PublicacaoPagamentoException.class);
        } finally {
            conexaoQuebrada.destroy();
        }

        assertThat(solicitacoes.buscarPorId(solicitacaoId).orElseThrow().status())
                .isEqualTo(SolicitacaoPagamento.Status.PENDENTE_PUBLICACAO);
        assertThat(pedidos.buscarPorId(pedido.id()).orElseThrow().status()).isEqualTo(StatusPedido.EM_PAGAMENTO);
        assertThat(mensagensDaSolicitacao(solicitacaoId)).isEmpty();

        solicitarPorHttp(pedido.id(), solicitacaoId, "APROVAR").andExpect(status().isAccepted());

        assertThat(solicitacoes.buscarPorId(solicitacaoId).orElseThrow().status())
                .isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
        assertThat(mensagensDaSolicitacao(solicitacaoId)).hasSize(1);
    }

    @Test
    void topologiaDeclaradaComFilasDeErroEArgumentosDeDeadLetter() throws Exception {
        for (var exchange : List.of("pagamentos.comandos", "pagamentos.eventos")) {
            var json = painel("/exchanges/%2F/" + exchange);
            assertThat(json.get("type").asString()).isEqualTo("direct");
            assertThat(json.get("durable").asBoolean()).isTrue();
        }
        assertFilaPrincipal("pagamentos.solicitacoes", "pagamentos.solicitacoes.erros");
        assertFilaPrincipal("pedidos.resultados", "pedidos.resultados.erros");
        for (var fila : List.of("pagamentos.solicitacoes.erros", "pedidos.resultados.erros")) {
            assertThat(painel("/queues/%2F/" + fila).get("durable").asBoolean()).isTrue();
        }
        assertBinding("pagamentos.comandos", "pagamentos.solicitacoes", "pagamento.solicitar");
        assertBinding("pagamentos.eventos", "pedidos.resultados", "pagamento.resultado");
    }

    private void assertFilaPrincipal(String fila, String filaDeErros) throws Exception {
        var json = painel("/queues/%2F/" + fila);
        assertThat(json.get("durable").asBoolean()).isTrue();
        assertThat(json.get("arguments").get("x-dead-letter-exchange").asString()).isEmpty();
        assertThat(json.get("arguments").get("x-dead-letter-routing-key").asString()).isEqualTo(filaDeErros);
    }

    private void assertBinding(String exchange, String fila, String chave) throws Exception {
        var bindings = painel("/exchanges/%2F/" + exchange + "/bindings/source");
        var chaves = new ArrayList<String>();
        bindings.forEach(binding -> {
            if (binding.get("destination").asString().equals(fila)) {
                chaves.add(binding.get("routing_key").asString());
            }
        });
        assertThat(chaves).containsExactly(chave);
    }

    private ResultActions solicitarPorHttp(UUID pedidoId, UUID solicitacaoId, String cenario) throws Exception {
        return mvc.perform(post("/pedidos/{pedidoId}/pagamentos", pedidoId)
                .contentType(APPLICATION_JSON)
                .content("""
                        {"solicitacaoId":"%s","cenario":"%s"}
                        """.formatted(solicitacaoId, cenario)));
    }

    /**
     * Esvazia a fila espiã e devolve as cópias publicadas para a solicitação.
     */
    private List<Message> mensagensDaSolicitacao(UUID solicitacaoId) {
        var encontradas = new ArrayList<Message>();
        Message mensagem;
        while ((mensagem = rabbitTemplate.receive(filaEspia, 1_000)) != null) {
            var corpo = jsonMapper.readTree(mensagem.getBody());
            if (solicitacaoId.toString().equals(corpo.path("solicitacaoId").asString(""))) {
                encontradas.add(mensagem);
            }
        }
        return encontradas;
    }

    private JsonNode painel(String caminho) throws Exception {
        var autorizacao = Base64.getEncoder().encodeToString(CREDENCIAL_LOCAL.getBytes(StandardCharsets.UTF_8));
        var resposta = http.send(HttpRequest.newBuilder(URI.create(PAINEL + caminho))
                        .header("Authorization", "Basic " + autorizacao).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(resposta.statusCode()).isEqualTo(200);
        return jsonMapper.readTree(resposta.body());
    }

    private static Pedido pedidoComCafe() {
        return Pedido.novo("c-pagamento")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));
    }
}
