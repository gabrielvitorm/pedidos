package br.com.pedidos.api.adapter.entrada.mensageria;

import br.com.pedidos.api.application.exception.ResultadoPagamentoInvalidoException;
import br.com.pedidos.api.application.port.in.RegistrarResultadoPagamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Aplica resultados de pagamento. O ack só ocorre depois que o resultado foi gravado;
 * reentrega após falha é segura porque resultado repetido não tem efeito.
 */
@Component
public class ResultadoPagamentoListener {

    static final String FILA_RESULTADOS = "pedidos.resultados";

    private static final Logger log = LoggerFactory.getLogger(ResultadoPagamentoListener.class);

    private final RegistrarResultadoPagamento registrarResultado;
    private final JsonMapper jsonMapper;

    public ResultadoPagamentoListener(RegistrarResultadoPagamento registrarResultado, JsonMapper jsonMapper) {
        this.registrarResultado = registrarResultado;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = FILA_RESULTADOS)
    public void receber(Message mensagem) {
        var resultado = ler(mensagem);
        try {
            registrarResultado.registrar(resultado.solicitacaoId(), resultado.pedidoId(), resultado.valor(),
                    resultado.aprovado(), resultado.motivo());
        } catch (ResultadoPagamentoInvalidoException exception) {
            log.warn("Resultado de pagamento recusado e enviado para a fila de erro: {}", exception.getMessage());
            throw new AmqpRejectAndDontRequeueException(exception.getMessage(), exception);
        }
    }

    ResultadoPagamentoMensagem ler(Message mensagem) {
        ResultadoPagamentoMensagem resultado;
        try {
            resultado = jsonMapper.readValue(mensagem.getBody(), ResultadoPagamentoMensagem.class);
        } catch (JacksonException exception) {
            log.warn("Resultado de pagamento ilegivel enviado para a fila de erro: {}", exception.getMessage());
            throw new AmqpRejectAndDontRequeueException("Resultado de pagamento ilegivel", exception);
        }
        if (!resultado.valida()) {
            log.warn("Resultado de pagamento invalido enviado para a fila de erro: {}", resultado);
            throw new AmqpRejectAndDontRequeueException("Resultado de pagamento invalido");
        }
        return resultado;
    }
}
