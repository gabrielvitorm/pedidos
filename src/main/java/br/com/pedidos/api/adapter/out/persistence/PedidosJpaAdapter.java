package br.com.pedidos.api.adapter.out.persistence;

import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.model.Pedido;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class PedidosJpaAdapter implements Pedidos {

    private final PedidoJpaRepository repository;
    private final PedidoJpaMapper mapper;

    public PedidosJpaAdapter(PedidoJpaRepository repository) {
        this.repository = repository;
        this.mapper = new PedidoJpaMapper();
    }

    @Override
    @Transactional
    public Pedido salvar(Pedido pedido) {
        var entity = repository.save(mapper.toEntity(pedido));
        repository.flush();
        return mapper.toDomain(entity);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Pedido> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
