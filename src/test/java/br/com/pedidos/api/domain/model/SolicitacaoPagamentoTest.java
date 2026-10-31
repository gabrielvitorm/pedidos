package br.com.pedidos.api.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SolicitacaoPagamentoTest {

    private static final UUID SOLICITACAO = UUID.randomUUID();
    private static final UUID PEDIDO = UUID.randomUUID();

    @Test
    void criaSolicitacaoPendente() {
        var solicitacao = SolicitacaoPagamento.pendente(SOLICITACAO, PEDIDO, new BigDecimal("37.80"),
                CenarioPagamento.APROVAR);

        assertThat(solicitacao.status()).isEqualTo(SolicitacaoPagamento.Status.PENDENTE_PUBLICACAO);
        assertThat(solicitacao.pendenteDePublicacao()).isTrue();
        assertThat(solicitacao.valor()).isEqualByComparingTo("37.80");
    }

    @Test
    void publicadaPreservaConteudoSemAlterarOriginal() {
        var pendente = SolicitacaoPagamento.pendente(SOLICITACAO, PEDIDO, new BigDecimal("37.80"),
                CenarioPagamento.RECUSAR);

        var publicada = pendente.publicada();

        assertThat(pendente.pendenteDePublicacao()).isTrue();
        assertThat(publicada.status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
        assertThat(publicada.solicitacaoId()).isEqualTo(SOLICITACAO);
        assertThat(publicada.pedidoId()).isEqualTo(PEDIDO);
        assertThat(publicada.valor()).isEqualByComparingTo("37.80");
        assertThat(publicada.cenario()).isEqualTo(CenarioPagamento.RECUSAR);
    }

    @Test
    void comparaConteudoPorPedidoECenario() {
        var solicitacao = SolicitacaoPagamento.pendente(SOLICITACAO, PEDIDO, new BigDecimal("37.80"),
                CenarioPagamento.APROVAR);

        assertThat(solicitacao.mesmoConteudo(PEDIDO, CenarioPagamento.APROVAR)).isTrue();
        assertThat(solicitacao.mesmoConteudo(PEDIDO, CenarioPagamento.RECUSAR)).isFalse();
        assertThat(solicitacao.mesmoConteudo(UUID.randomUUID(), CenarioPagamento.APROVAR)).isFalse();
    }

    @Test
    void resolveComResultadoSemAlterarOriginal() {
        var publicada = SolicitacaoPagamento.pendente(SOLICITACAO, PEDIDO, new BigDecimal("37.80"),
                CenarioPagamento.APROVAR).publicada();

        var resolvida = publicada.resolver(true, "Aprovado pelo simulador");

        assertThat(publicada.resolvida()).isFalse();
        assertThat(resolvida.resolvida()).isTrue();
        assertThat(resolvida.aprovado()).isTrue();
        assertThat(resolvida.motivo()).isEqualTo("Aprovado pelo simulador");
        assertThat(resolvida.status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
    }

    @Test
    void naoResolveDuasVezes() {
        var resolvida = SolicitacaoPagamento.pendente(SOLICITACAO, PEDIDO, new BigDecimal("37.80"),
                CenarioPagamento.RECUSAR).resolver(false, "Recusado pelo simulador");

        assertThatIllegalStateException().isThrownBy(() -> resolvida.resolver(false, "Recusado pelo simulador"));
    }

    @Test
    void recusaValorNaoPositivo() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                SolicitacaoPagamento.pendente(SOLICITACAO, PEDIDO, BigDecimal.ZERO, CenarioPagamento.APROVAR));
        assertThatIllegalArgumentException().isThrownBy(() ->
                SolicitacaoPagamento.pendente(SOLICITACAO, PEDIDO, new BigDecimal("-1.00"), CenarioPagamento.APROVAR));
    }

    @Test
    void recusaCamposObrigatoriosAusentes() {
        var valor = new BigDecimal("37.80");

        assertThatIllegalArgumentException().isThrownBy(() ->
                SolicitacaoPagamento.pendente(null, PEDIDO, valor, CenarioPagamento.APROVAR));
        assertThatIllegalArgumentException().isThrownBy(() ->
                SolicitacaoPagamento.pendente(SOLICITACAO, null, valor, CenarioPagamento.APROVAR));
        assertThatIllegalArgumentException().isThrownBy(() ->
                SolicitacaoPagamento.pendente(SOLICITACAO, PEDIDO, null, CenarioPagamento.APROVAR));
        assertThatIllegalArgumentException().isThrownBy(() ->
                SolicitacaoPagamento.pendente(SOLICITACAO, PEDIDO, valor, null));
    }
}
