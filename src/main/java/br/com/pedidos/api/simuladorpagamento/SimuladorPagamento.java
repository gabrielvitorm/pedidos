package br.com.pedidos.api.simuladorpagamento;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Processador de pagamentos simulado. Não realiza cobrança e não acessa dados de pedidos:
 * o resultado depende somente do cenário recebido, então reprocessar produz o mesmo resultado.
 */
public class SimuladorPagamento {

    static final int VERSAO = 1;

    public ResultadoEmitido simular(SolicitacaoRecebida solicitacao) {
        var aprovado = "APROVAR".equals(solicitacao.cenario());
        var motivo = aprovado ? "Aprovado pelo simulador" : "Recusado pelo simulador";
        return new ResultadoEmitido(VERSAO, solicitacao.solicitacaoId(), solicitacao.pedidoId(),
                solicitacao.valor(), aprovado, motivo);
    }

    public record SolicitacaoRecebida(int versao, UUID solicitacaoId, UUID pedidoId, BigDecimal valor,
                                      String cenario) {

        public boolean valida() {
            return versao == VERSAO
                    && solicitacaoId != null
                    && pedidoId != null
                    && valor != null
                    && ("APROVAR".equals(cenario) || "RECUSAR".equals(cenario));
        }
    }

    public record ResultadoEmitido(int versao, UUID solicitacaoId, UUID pedidoId, BigDecimal valor,
                                   boolean aprovado, String motivo) {
    }
}
