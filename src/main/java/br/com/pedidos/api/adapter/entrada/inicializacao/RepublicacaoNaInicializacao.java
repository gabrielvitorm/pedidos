package br.com.pedidos.api.adapter.entrada.inicializacao;

import br.com.pedidos.api.application.port.in.RepublicarPagamentosPendentes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ao iniciar, republica solicitações de pagamento que ficaram pendentes de publicação.
 * Uma falha aqui não impede a aplicação de subir: as pendentes continuam gravadas para a próxima tentativa.
 */
@Component
public class RepublicacaoNaInicializacao {

    private static final Logger log = LoggerFactory.getLogger(RepublicacaoNaInicializacao.class);

    private final RepublicarPagamentosPendentes republicarPendentes;

    public RepublicacaoNaInicializacao(RepublicarPagamentosPendentes republicarPendentes) {
        this.republicarPendentes = republicarPendentes;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void republicar() {
        try {
            var resultado = republicarPendentes.republicar();
            if (resultado.republicadas() > 0 || resultado.falhas() > 0) {
                log.info("Solicitacoes de pagamento pendentes republicadas: {}, falhas: {}",
                        resultado.republicadas(), resultado.falhas());
            }
        } catch (RuntimeException exception) {
            log.warn("Nao foi possivel republicar solicitacoes de pagamento pendentes", exception);
        }
    }
}
