package br.com.pedidos.api.adapter.out.persistence;

import br.com.pedidos.api.domain.model.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PedidoJpaRepository extends JpaRepository<PedidoJpaEntity, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PedidoJpaEntity p set p.status = :novo where p.id = :id and p.status = :esperado")
    int alterarStatus(@Param("id") UUID id,
                      @Param("esperado") StatusPedido esperado,
                      @Param("novo") StatusPedido novo);
}
