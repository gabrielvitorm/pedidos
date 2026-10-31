package br.com.pedidos.api.config;

import br.com.pedidos.api.application.port.in.CriarPedido;
import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.application.port.in.ConsultarPedido;
import br.com.pedidos.api.application.port.in.RegistrarResultadoPagamento;
import br.com.pedidos.api.application.port.in.SolicitarPagamento;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.PublicadorPagamentos;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.application.service.AdicionarItemService;
import br.com.pedidos.api.application.service.ConsultarPedidoService;
import br.com.pedidos.api.application.service.RegistrarResultadoPagamentoService;
import br.com.pedidos.api.application.service.SolicitarPagamentoService;
import br.com.pedidos.api.application.service.CriarPedidoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CasosDeUsoConfig {

    @Bean
    CriarPedido criarPedido(Pedidos pedidos) {
        return new CriarPedidoService(pedidos);
    }

    @Bean
    AdicionarItem adicionarItem(Pedidos pedidos) {
        return new AdicionarItemService(pedidos);
    }

    @Bean
    ConsultarPedido consultarPedido(Pedidos pedidos) {
        return new ConsultarPedidoService(pedidos);
    }

    @Bean
    SolicitarPagamento solicitarPagamento(Pedidos pedidos, SolicitacoesPagamento solicitacoes,
                                          PublicadorPagamentos publicador) {
        return new SolicitarPagamentoService(pedidos, solicitacoes, publicador);
    }

    @Bean
    RegistrarResultadoPagamento registrarResultadoPagamento(Pedidos pedidos, SolicitacoesPagamento solicitacoes) {
        return new RegistrarResultadoPagamentoService(pedidos, solicitacoes);
    }
}
