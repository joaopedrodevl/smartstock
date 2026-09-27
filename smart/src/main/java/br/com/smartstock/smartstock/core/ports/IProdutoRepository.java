package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.Produto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IProdutoRepository {
    Optional<Produto> findById(Long id);
    Optional<Produto> findByPublicId(UUID publicId);
    List<Produto> findByLojaId(Long lojaId);
    Long save(Produto produto);
    void update(Produto produto);
    void deleteByPublicId(UUID publicId);
}
