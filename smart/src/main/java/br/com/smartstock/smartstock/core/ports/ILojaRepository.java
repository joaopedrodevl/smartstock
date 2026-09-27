package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.Loja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ILojaRepository {
    Long save(Loja loja);
    Optional<Loja> findById(Long id);
    Optional<Loja> findByPublicId(UUID publicId);
    List<Loja> findByAdministradorId(Long administradorId);
    List<Loja> findByAdministradorId(Long administradorId, int page, int size);
    long countByAdministradorId(Long administradorId);
    boolean existsByCnpj(String cnpj);
    boolean existsByCnpjAndIdNot(String cnpj, Long id);
    void update(Loja loja);
    void delete(Long id);
}
