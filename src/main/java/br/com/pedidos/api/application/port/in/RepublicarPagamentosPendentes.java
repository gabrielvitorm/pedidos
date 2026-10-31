package br.com.pedidos.api.application.port.in;

public interface RepublicarPagamentosPendentes {

    Resultado republicar();

    record Resultado(int republicadas, int falhas) {
    }
}
