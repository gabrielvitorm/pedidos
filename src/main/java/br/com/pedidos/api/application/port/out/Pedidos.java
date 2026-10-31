package br.com.pedidos.api.application.port.out;

import br.com.pedidos.api.domain.model.Pedido;

import java.util.Optional;
import java.util.UUID;

public interface Pedidos {

    Pedido salvar(Pedido pedido);

    Optional<Pedido> buscarPorId(UUID id);
}
