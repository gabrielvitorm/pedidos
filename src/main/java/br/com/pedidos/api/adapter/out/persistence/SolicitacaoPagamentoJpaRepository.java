package br.com.pedidos.api.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SolicitacaoPagamentoJpaRepository extends JpaRepository<SolicitacaoPagamentoJpaEntity, UUID> {
}
