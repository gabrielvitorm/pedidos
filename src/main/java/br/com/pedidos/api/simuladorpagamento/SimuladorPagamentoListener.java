package br.com.pedidos.api.simuladorpagamento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

/**
 * Lado do serviço de pagamento: consome solicitações e publica resultados.
 * Mensagem inválida vai para a fila de erro; falha ao publicar devolve a solicitação à fila.
 */
@Component
public class SimuladorPagamentoListener {

    static final String FILA_SOLICITACOES = "pagamentos.solicitacoes";
    static final String EXCHANGE_EVENTOS = "pagamentos.eventos";
    static final String CHAVE_RESULTADO = "pagamento.resultado";
    private static final long TIMEOUT_CONFIRMACAO_MS = 5_000;

    private static final Logger log = LoggerFactory.getLogger(SimuladorPagamentoListener.class);

    private final SimuladorPagamento simulador = new SimuladorPagamento();
    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    public SimuladorPagamentoListener(RabbitTemplate rabbitTemplate, JsonMapper jsonMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = FILA_SOLICITACOES)
    public void receber(Message mensagem) throws Exception {
        var resultado = simulador.simular(ler(mensagem));
        publicar(resultado);
    }

    SimuladorPagamento.SolicitacaoRecebida ler(Message mensagem) {
        SimuladorPagamento.SolicitacaoRecebida solicitacao;
        try {
            solicitacao = jsonMapper.readValue(mensagem.getBody(), SimuladorPagamento.SolicitacaoRecebida.class);
        } catch (JacksonException exception) {
            log.warn("Solicitacao de pagamento ilegivel enviada para a fila de erro: {}", exception.getMessage());
            throw new AmqpRejectAndDontRequeueException("Solicitacao de pagamento ilegivel", exception);
        }
        if (!solicitacao.valida()) {
            log.warn("Solicitacao de pagamento invalida enviada para a fila de erro: {}", solicitacao);
            throw new AmqpRejectAndDontRequeueException("Solicitacao de pagamento invalida");
        }
        return solicitacao;
    }

    private void publicar(SimuladorPagamento.ResultadoEmitido resultado) throws Exception {
        var correlacao = new CorrelationData(resultado.solicitacaoId().toString());
        var mensagem = MessageBuilder.withBody(jsonMapper.writeValueAsBytes(resultado))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(resultado.solicitacaoId().toString())
                .build();
        rabbitTemplate.send(EXCHANGE_EVENTOS, CHAVE_RESULTADO, mensagem, correlacao);
        var confirmacao = correlacao.getFuture().get(TIMEOUT_CONFIRMACAO_MS, TimeUnit.MILLISECONDS);
        if (!confirmacao.ack() || correlacao.getReturned() != null) {
            throw new IllegalStateException("Resultado nao confirmado ou nao roteado; solicitacao volta para a fila");
        }
    }
}
