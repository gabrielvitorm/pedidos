package br.com.pedidos.api.adapter.out.mensageria;

import br.com.pedidos.api.application.exception.PublicacaoPagamentoException;
import br.com.pedidos.api.application.port.out.PublicadorPagamentos;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PublicadorPagamentosRabbit implements PublicadorPagamentos {

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;
    private final String exchange;
    private final String chave;
    private final Duration timeout;

    public PublicadorPagamentosRabbit(RabbitTemplate rabbitTemplate, JsonMapper jsonMapper,
                                      String exchange, String chave, Duration timeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
        this.exchange = exchange;
        this.chave = chave;
        this.timeout = timeout;
    }

    @Override
    public void publicar(SolicitacaoPagamento solicitacao) {
        var correlacao = new CorrelationData(solicitacao.solicitacaoId().toString());
        try {
            rabbitTemplate.send(exchange, chave, mensagem(solicitacao), correlacao);
            var confirmacao = correlacao.getFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirmacao.ack()) {
                throw new PublicacaoPagamentoException("Broker recusou a publicacao: " + confirmacao.reason());
            }
            if (correlacao.getReturned() != null) {
                throw new PublicacaoPagamentoException("Mensagem nao roteada: " + correlacao.getReturned().getReplyText());
            }
        } catch (AmqpException exception) {
            throw new PublicacaoPagamentoException("Falha ao publicar no broker", exception);
        } catch (TimeoutException exception) {
            throw new PublicacaoPagamentoException("Broker nao confirmou a publicacao a tempo", exception);
        } catch (ExecutionException exception) {
            throw new PublicacaoPagamentoException("Falha ao aguardar confirmacao do broker", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicacaoPagamentoException("Publicacao interrompida", exception);
        }
    }

    private Message mensagem(SolicitacaoPagamento solicitacao) {
        return MessageBuilder.withBody(jsonMapper.writeValueAsBytes(SolicitacaoPagamentoMensagem.from(solicitacao)))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(solicitacao.solicitacaoId().toString())
                .build();
    }
}
