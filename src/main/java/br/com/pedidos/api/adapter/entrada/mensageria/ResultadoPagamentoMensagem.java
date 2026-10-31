package br.com.pedidos.api.adapter.entrada.mensageria;

import java.math.BigDecimal;
import java.util.UUID;

public record ResultadoPagamentoMensagem(
        int versao,
        UUID solicitacaoId,
        UUID pedidoId,
        BigDecimal valor,
        Boolean aprovado,
        String motivo
) {

    static final int VERSAO_ATUAL = 1;

    boolean valida() {
        return versao == VERSAO_ATUAL
                && solicitacaoId != null
                && pedidoId != null
                && valor != null
                && aprovado != null;
    }
}
