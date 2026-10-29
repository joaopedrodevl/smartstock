package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.Endereco;

import java.util.Optional;
import java.util.UUID;

public interface IEnderecoRepository {
    Long save(Endereco endereco);
    Optional<Endereco> findById(Long id);
    Optional<Endereco> findByPublicId(UUID publicId);
    void update(Endereco endereco);
    void delete(Long id);
}
