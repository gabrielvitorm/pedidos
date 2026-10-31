package br.com.pedidos.api.application.port.in;

import br.com.pedidos.api.domain.model.CenarioPagamento;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;

import java.util.UUID;

public interface SolicitarPagamento {

    SolicitacaoPagamento solicitar(UUID pedidoId, UUID solicitacaoId, CenarioPagamento cenario);
}
