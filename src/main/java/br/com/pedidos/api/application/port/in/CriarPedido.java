package br.com.pedidos.api.application.port.in;

import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;

import java.util.List;

public interface CriarPedido {

    Pedido criar(String cliente, List<ItemPedido> itens);
}
