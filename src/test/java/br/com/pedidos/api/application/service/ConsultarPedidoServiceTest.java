package br.com.pedidos.api.application.service;

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

class ConsultarPedidoServiceTest {

    @Test
    void devolvePedidoExistente() {
        var existente = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));
        var service = new ConsultarPedidoService(new PedidosEmMemoria(existente));

        var resultado = service.consultar(existente.id());

        assertThat(resultado).isEqualTo(existente);
        assertThat(resultado.total()).isEqualByComparingTo("37.80");
    }

    @Test
    void pedidoInexistenteLancaNaoEncontrado() {
        var service = new ConsultarPedidoService(new PedidosEmMemoria(null));
        var id = UUID.randomUUID();

        assertThatThrownBy(() -> service.consultar(id))
                .isInstanceOf(PedidoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }

    private static final class PedidosEmMemoria implements Pedidos {

        private final Pedido pedido;

        private PedidosEmMemoria(Pedido pedido) {
            this.pedido = pedido;
        }

        @Override
        public Optional<Pedido> buscarPorId(UUID id) {
            return Optional.ofNullable(pedido).filter(item -> item.id().equals(id));
        }

        @Override
        public Pedido salvar(Pedido pedido) {
            throw new UnsupportedOperationException("Consulta não deve salvar");
        }
    }
}
