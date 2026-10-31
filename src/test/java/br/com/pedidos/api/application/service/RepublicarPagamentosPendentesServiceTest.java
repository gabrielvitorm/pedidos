package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PublicacaoPagamentoException;
import br.com.pedidos.api.application.port.out.PublicadorPagamentos;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.Pedido;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RepublicarPagamentosPendentesServiceTest {

    private final SolicitacoesEmMemoria solicitacoes = new SolicitacoesEmMemoria();
    private final PublicadorFalso publicador = new PublicadorFalso();
    private final RepublicarPagamentosPendentesService service =
            new RepublicarPagamentosPendentesService(solicitacoes, publicador);

    @Test
    void republicaTodasAsPendentesEMarcaPublicadas() {
        var primeira = solicitacoes.pendente();
        var segunda = solicitacoes.pendente();

        var resultado = service.republicar();

        assertThat(resultado.republicadas()).isEqualTo(2);
        assertThat(resultado.falhas()).isZero();
        assertThat(publicador.publicadas).containsExactly(primeira.solicitacaoId(), segunda.solicitacaoId());
        assertThat(solicitacoes.porId(primeira.solicitacaoId()).status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
        assertThat(solicitacoes.porId(segunda.solicitacaoId()).status()).isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
    }

    @Test
    void falhaEmUmaMantemPendenteENaoImpedeAsDemais() {
        var falha = solicitacoes.pendente();
        var sucesso = solicitacoes.pendente();
        publicador.falharPara = Set.of(falha.solicitacaoId());

        var resultado = service.republicar();

        assertThat(resultado.republicadas()).isEqualTo(1);
        assertThat(resultado.falhas()).isEqualTo(1);
        assertThat(solicitacoes.porId(falha.solicitacaoId()).status())
                .isEqualTo(SolicitacaoPagamento.Status.PENDENTE_PUBLICACAO);
        assertThat(solicitacoes.porId(sucesso.solicitacaoId()).status())
                .isEqualTo(SolicitacaoPagamento.Status.PUBLICADA);
    }

    @Test
    void semPendentesNaoPublicaNada() {
        var resultado = service.republicar();

        assertThat(resultado.republicadas()).isZero();
        assertThat(resultado.falhas()).isZero();
        assertThat(publicador.publicadas).isEmpty();
    }

    private static final class SolicitacoesEmMemoria implements SolicitacoesPagamento {

        private final Map<UUID, SolicitacaoPagamento> solicitacoes = new LinkedHashMap<>();

        private SolicitacaoPagamento pendente() {
            var solicitacao = SolicitacaoPagamento.pendente(UUID.randomUUID(), UUID.randomUUID(),
                    new BigDecimal("37.80"), CenarioPagamento.APROVAR);
            solicitacoes.put(solicitacao.solicitacaoId(), solicitacao);
            return solicitacao;
        }

        @Override
        public List<SolicitacaoPagamento> listarPendentesDePublicacao() {
            return solicitacoes.values().stream()
                    .filter(solicitacao -> solicitacao.pendenteDePublicacao() && !solicitacao.resolvida())
                    .toList();
        }

        @Override
        public SolicitacaoPagamento marcarPublicada(UUID solicitacaoId) {
            var publicada = solicitacoes.get(solicitacaoId).publicada();
            solicitacoes.put(solicitacaoId, publicada);
            return publicada;
        }

        @Override
        public Optional<SolicitacaoPagamento> buscarPorId(UUID solicitacaoId) {
            return Optional.ofNullable(solicitacoes.get(solicitacaoId));
        }

        @Override
        public void registrar(Pedido pedidoEmPagamento, SolicitacaoPagamento solicitacao) {
            throw new UnsupportedOperationException("Republicar nao registra solicitacao");
        }

        @Override
        public void registrarResultado(Pedido pedidoAtualizado, SolicitacaoPagamento solicitacaoResolvida) {
            throw new UnsupportedOperationException("Republicar nao registra resultado");
        }

        private SolicitacaoPagamento porId(UUID solicitacaoId) {
            return solicitacoes.get(solicitacaoId);
        }
    }

    private static final class PublicadorFalso implements PublicadorPagamentos {

        private final List<UUID> publicadas = new ArrayList<>();
        private Set<UUID> falharPara = Set.of();

        @Override
        public void publicar(SolicitacaoPagamento solicitacao) {
            if (falharPara.contains(solicitacao.solicitacaoId())) {
                throw new PublicacaoPagamentoException("Broker nao confirmou a publicacao");
            }
            publicadas.add(solicitacao.solicitacaoId());
        }
    }
}
