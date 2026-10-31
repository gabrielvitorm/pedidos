package br.com.pedidos.api.adapter.out.mensageria;

import br.com.pedidos.api.application.port.out.PublicadorPagamentos;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Configuration
public class MensageriaPagamentosConfig {

    private static final Duration TIMEOUT_CONFIRMACAO = Duration.ofSeconds(5);

    @Bean
    PublicadorPagamentos publicadorPagamentos(RabbitTemplate rabbitTemplate, JsonMapper jsonMapper) {
        return new PublicadorPagamentosRabbit(rabbitTemplate, jsonMapper,
                TopologiaPagamentosConfig.EXCHANGE_COMANDOS, TopologiaPagamentosConfig.CHAVE_SOLICITAR,
                TIMEOUT_CONFIRMACAO);
    }
}
