package br.com.pedidos.api.application.exception;

import java.util.UUID;

public class SolicitacaoConflitanteException extends RuntimeException {

    public SolicitacaoConflitanteException(UUID solicitacaoId) {
        super("Solicitacao " + solicitacaoId + " ja foi usada com outro conteudo");
    }
}
