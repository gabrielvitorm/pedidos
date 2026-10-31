package br.com.pedidos.api.application.port.in;

import br.com.pedidos.api.domain.model.Pedido;

import java.util.UUID;

public interface ConsultarPedido {

    Pedido consultar(UUID id);
}
