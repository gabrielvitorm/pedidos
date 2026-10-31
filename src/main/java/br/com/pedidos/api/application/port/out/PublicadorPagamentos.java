package br.com.pedidos.api.application.port.out;

import br.com.pedidos.api.application.exception.PublicacaoPagamentoException;
import br.com.pedidos.api.domain.model.SolicitacaoPagamento;

public interface PublicadorPagamentos {

    /**
     * Retorna somente após o broker confirmar e rotear a mensagem.
     *
     * @throws PublicacaoPagamentoException quando não houver confirmação ou roteamento
     */
    void publicar(SolicitacaoPagamento solicitacao);
}
