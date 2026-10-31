package br.com.pedidos.api.simuladorpagamento;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimuladorPagamentoTest {

    private final SimuladorPagamento simulador = new SimuladorPagamento();
    private final SimuladorPagamentoListener listener =
            new SimuladorPagamentoListener(null, JsonMapper.builder().build());

    @Test
    void cenarioAprovarGeraResultadoAprovadoComMesmosDados() {
        var solicitacao = solicitacao("APROVAR");

        var resultado = simulador.simular(solicitacao);

        assertThat(resultado.versao()).isEqualTo(1);
        assertThat(resultado.solicitacaoId()).isEqualTo(solicitacao.solicitacaoId());
        assertThat(resultado.pedidoId()).isEqualTo(solicitacao.pedidoId());
        assertThat(resultado.valor()).isEqualByComparingTo("37.80");
        assertThat(resultado.aprovado()).isTrue();
        assertThat(resultado.motivo()).isEqualTo("Aprovado pelo simulador");
    }

    @Test
    void cenarioRecusarGeraResultadoRecusadoComMotivo() {
        var resultado = simulador.simular(solicitacao("RECUSAR"));

        assertThat(resultado.aprovado()).isFalse();
        assertThat(resultado.motivo()).isEqualTo("Recusado pelo simulador");
    }

    @Test
    void reprocessarMesmaSolicitacaoGeraMesmoResultado() {
        var solicitacao = solicitacao("APROVAR");

        assertThat(simulador.simular(solicitacao)).isEqualTo(simulador.simular(solicitacao));
    }

    @Test
    void leSolicitacaoV1Valida() {
        var id = UUID.randomUUID();

        var lida = listener.ler(mensagem("""
                {"versao":1,"solicitacaoId":"%s","pedidoId":"%s","valor":37.80,"cenario":"RECUSAR"}
                """.formatted(id, UUID.randomUUID())));

        assertThat(lida.solicitacaoId()).isEqualTo(id);
        assertThat(lida.valor()).isEqualByComparingTo("37.80");
        assertThat(lida.cenario()).isEqualTo("RECUSAR");
    }

    @Test
    void jsonIlegivelVaiParaFilaDeErro() {
        assertThatThrownBy(() -> listener.ler(mensagem("nao-json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void versaoDiferenteVaiParaFilaDeErro() {
        assertThatThrownBy(() -> listener.ler(mensagem("""
                {"versao":2,"solicitacaoId":"%s","pedidoId":"%s","valor":37.80,"cenario":"APROVAR"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()))))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    @Test
    void cenarioDesconhecidoOuCampoAusenteVaiParaFilaDeErro() {
        assertThatThrownBy(() -> listener.ler(mensagem("""
                {"versao":1,"solicitacaoId":"%s","pedidoId":"%s","valor":37.80,"cenario":"TALVEZ"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()))))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        assertThatThrownBy(() -> listener.ler(mensagem("""
                {"versao":1,"pedidoId":"%s","valor":37.80,"cenario":"APROVAR"}
                """.formatted(UUID.randomUUID()))))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    private static SimuladorPagamento.SolicitacaoRecebida solicitacao(String cenario) {
        return new SimuladorPagamento.SolicitacaoRecebida(1, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("37.80"), cenario);
    }

    private static org.springframework.amqp.core.Message mensagem(String corpo) {
        return MessageBuilder.withBody(corpo.getBytes(StandardCharsets.UTF_8)).build();
    }
}
