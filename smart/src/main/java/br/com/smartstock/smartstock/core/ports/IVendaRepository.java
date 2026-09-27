package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.Venda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IVendaRepository {
    Optional<Venda> findById(Long id);
    Optional<Venda> findByPublicId(UUID publicId);
    Long save(Venda venda);
    void update(Venda venda);
    List<Venda> findByLojaId(Long lojaId);
    List<Venda> findByClienteId(Long clienteId);
}
