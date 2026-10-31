package br.com.pedidos.api.adapter.out.persistence;

import br.com.pedidos.api.domain.model.SolicitacaoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Publicação e resultado podem ser gravados ao mesmo tempo para a mesma solicitação.
 * Cada update altera somente as próprias colunas, para que um não desfaça o outro.
 */
public interface SolicitacaoPagamentoJpaRepository extends JpaRepository<SolicitacaoPagamentoJpaEntity, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update SolicitacaoPagamentoJpaEntity s set s.status = :status where s.solicitacaoId = :id")
    int alterarStatus(@Param("id") UUID id, @Param("status") SolicitacaoPagamento.Status status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update SolicitacaoPagamentoJpaEntity s set s.aprovado = :aprovado, s.motivo = :motivo
            where s.solicitacaoId = :id and s.aprovado is null
            """)
    int registrarResultado(@Param("id") UUID id, @Param("aprovado") boolean aprovado,
                           @Param("motivo") String motivo);
}
