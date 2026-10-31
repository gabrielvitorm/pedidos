package br.com.pedidos.api.adapter.out.mensageria;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TopologiaPagamentosConfig {

    public static final String EXCHANGE_COMANDOS = "pagamentos.comandos";
    public static final String CHAVE_SOLICITAR = "pagamento.solicitar";
    public static final String FILA_SOLICITACOES = "pagamentos.solicitacoes";
    public static final String FILA_SOLICITACOES_ERROS = "pagamentos.solicitacoes.erros";

    public static final String EXCHANGE_EVENTOS = "pagamentos.eventos";
    public static final String CHAVE_RESULTADO = "pagamento.resultado";
    public static final String FILA_RESULTADOS = "pedidos.resultados";
    public static final String FILA_RESULTADOS_ERROS = "pedidos.resultados.erros";

    @Bean
    Declarables topologiaPagamentos() {
        DirectExchange comandos = ExchangeBuilder.directExchange(EXCHANGE_COMANDOS).durable(true).build();
        DirectExchange eventos = ExchangeBuilder.directExchange(EXCHANGE_EVENTOS).durable(true).build();
        Queue solicitacoes = filaComErros(FILA_SOLICITACOES, FILA_SOLICITACOES_ERROS);
        Queue resultados = filaComErros(FILA_RESULTADOS, FILA_RESULTADOS_ERROS);

        return new Declarables(
                comandos,
                eventos,
                QueueBuilder.durable(FILA_SOLICITACOES_ERROS).build(),
                QueueBuilder.durable(FILA_RESULTADOS_ERROS).build(),
                solicitacoes,
                resultados,
                BindingBuilder.bind(solicitacoes).to(comandos).with(CHAVE_SOLICITAR),
                BindingBuilder.bind(resultados).to(eventos).with(CHAVE_RESULTADO));
    }

    /**
     * Os argumentos de dead-letter são definidos na primeira declaração da fila.
     * Mudanças futuras devem usar policy: redeclarar com outros argumentos falha.
     */
    private static Queue filaComErros(String nome, String filaDeErros) {
        return QueueBuilder.durable(nome)
                .deadLetterExchange("")
                .deadLetterRoutingKey(filaDeErros)
                .build();
    }
}
