package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.model.ItemPedido;
import br.com.pedidos.api.domain.model.Pedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CriarPedidoServiceTest {

    @Test
    void criaPedidoComClienteEItensESalvaOPedidoCompleto() {
        var repositorio = new PedidosEmMemoria();
        var pedidoSalvo = Pedido.novo("c-1");
        var service = new CriarPedidoService(repositorio);
        var itens = List.of(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));
        repositorio.pedidoDevolvido = pedidoSalvo;

        var resultado = service.criar("c-1", itens);

        assertThat(repositorio.pedidoRecebido.cliente()).isEqualTo("c-1");
        assertThat(repositorio.pedidoRecebido.itens()).containsExactlyElementsOf(itens);
        assertThat(repositorio.pedidoRecebido.status()).isEqualTo(br.com.pedidos.api.domain.model.StatusPedido.ABERTO);
        assertThat(resultado).isSameAs(pedidoSalvo);
        assertThat(repositorio.quantidadeDeChamadas).isEqualTo(1);
    }

    @Test
    void recusaListaVaziaESemChamarPedidos() {
        var repositorio = new PedidosEmMemoria();
        var service = new CriarPedidoService(repositorio);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.criar("c-1", List.of()));

        assertThat(repositorio.quantidadeDeChamadas).isZero();
    }

    private static final class PedidosEmMemoria implements Pedidos {
        private Pedido pedidoRecebido;
        private Pedido pedidoDevolvido;
        private int quantidadeDeChamadas;

        @Override
        public Pedido salvar(Pedido pedido) {
            quantidadeDeChamadas++;
            pedidoRecebido = pedido;
            return pedidoDevolvido;
        }

        @Override
        public Optional<Pedido> buscarPorId(UUID id) {
            return Optional.empty();
        }
    }
}
