package br.com.pedidos.api.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface RegistrarResultadoPagamento {

    void registrar(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, boolean aprovado, String motivo);
}
