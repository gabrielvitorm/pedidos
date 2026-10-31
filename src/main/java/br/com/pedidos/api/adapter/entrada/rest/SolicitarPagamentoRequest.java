package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.domain.model.CenarioPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.math.BigDecimal;
import java.util.UUID;

public record SolicitarPagamentoRequest(
        @NotNull UUID solicitacaoId,
        @NotNull CenarioPagamento cenario,
        // O valor vem do pedido; enviá-lo é requisição inválida
        @Null BigDecimal valor
) {
}
