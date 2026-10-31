package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.ItemInvalidoException;
import br.com.pedidos.api.application.exception.PedidoFechadoException;
import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdicionarItemServiceTest {

    @Test
    void adicionaItemPreservaUuidAtualizaTotalESalva() {
        var existente = pedidoComCafe();
        var pedidos = new PedidosEmMemoria(existente);
        var service = new AdicionarItemService(pedidos);

        var resultado = service.adicionar(existente.id(), "CAFE-500", 1, new BigDecimal("18.90"));

        assertThat(resultado.id()).isEqualTo(existente.id());
        assertThat(resultado.itens()).hasSize(2);
        assertThat(resultado.total()).isEqualByComparingTo("56.70");
        assertThat(pedidos.salvamentos).isEqualTo(1);
    }

    @Test
    void pedidoInexistenteNaoSalva() {
        var pedidos = new PedidosEmMemoria(null);
        var service = new AdicionarItemService(pedidos);

        assertThatThrownBy(() -> service.adicionar(UUID.randomUUID(), "CAFE-500", 1, new BigDecimal("18.90")))
                .isInstanceOf(PedidoNaoEncontradoException.class);
        assertThat(pedidos.salvamentos).isZero();
    }

    @Test
    void pedidoFechadoNaoSalva() {
        var existente = pedidoComCafe().pagar();
        var pedidos = new PedidosEmMemoria(existente);
        var service = new AdicionarItemService(pedidos);

        assertThatThrownBy(() -> service.adicionar(existente.id(), "CAFE-500", 1, new BigDecimal("18.90")))
                .isInstanceOf(PedidoFechadoException.class);
        assertThat(pedidos.salvamentos).isZero();
    }

    @Test
    void quantidadeInvalidaNaoSalva() {
        var existente = pedidoComCafe();
        var pedidos = new PedidosEmMemoria(existente);
        var service = new AdicionarItemService(pedidos);

        assertThatThrownBy(() -> service.adicionar(existente.id(), "CAFE-500", 0, new BigDecimal("18.90")))
                .isInstanceOf(ItemInvalidoException.class);
        assertThat(pedidos.salvamentos).isZero();
    }

    @Test
    void precoInvalidoNaoSalva() {
        var existente = pedidoComCafe();
        var pedidos = new PedidosEmMemoria(existente);
        var service = new AdicionarItemService(pedidos);

        assertThatThrownBy(() -> service.adicionar(existente.id(), "CAFE-500", 1, BigDecimal.ZERO))
                .isInstanceOf(ItemInvalidoException.class);
        assertThat(pedidos.salvamentos).isZero();
    }

    private static Pedido pedidoComCafe() {
        return Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));
    }

    private static final class PedidosEmMemoria implements Pedidos {

        private Pedido pedido;
        private int salvamentos;

        private PedidosEmMemoria(Pedido pedido) {
            this.pedido = pedido;
        }

        @Override
        public Optional<Pedido> buscarPorId(UUID id) {
            return Optional.ofNullable(pedido).filter(item -> item.id().equals(id));
        }

        @Override
        public Pedido salvar(Pedido pedido) {
            salvamentos++;
            this.pedido = pedido;
            return pedido;
        }
    }
}
