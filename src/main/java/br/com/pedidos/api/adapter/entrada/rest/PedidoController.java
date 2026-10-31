package br.com.pedidos.api.adapter.entrada.rest;

import br.com.pedidos.api.application.exception.ItemInvalidoException;
import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.application.port.in.CriarPedido;
import br.com.pedidos.api.domain.model.ItemPedido;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final CriarPedido criarPedido;
    private final AdicionarItem adicionarItem;

    public PedidoController(CriarPedido criarPedido, AdicionarItem adicionarItem) {
        this.criarPedido = criarPedido;
        this.adicionarItem = adicionarItem;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(@Valid @RequestBody PedidoRequest request) {
        var itens = request.itens().stream()
                .map(item -> {
                    try {
                        return new ItemPedido(item.sku(), item.quantidade(), item.precoUnitario());
                    } catch (IllegalArgumentException exception) {
                        throw new ItemInvalidoException(exception.getMessage(), exception);
                    }
                })
                .toList();
        return PedidoResponse.from(criarPedido.criar(request.clienteId(), itens));
    }

    @PostMapping("/{id}/itens")
    public PedidoResponse adicionarItem(@PathVariable UUID id,
                                        @Valid @RequestBody AdicionarItemRequest request) {
        var pedido = adicionarItem.adicionar(id, request.sku(), request.quantidade(), request.precoUnitario());
        return PedidoResponse.from(pedido);
    }
}
