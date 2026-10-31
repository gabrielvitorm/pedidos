package br.com.pedidos.api.application.port.in;

import br.com.pedidos.api.domain.model.Pedido;

import java.math.BigDecimal;
import java.util.UUID;

public interface AdicionarItem {

    Pedido adicionar(UUID pedidoId, String sku, int quantidade, BigDecimal precoUnitario);
}
