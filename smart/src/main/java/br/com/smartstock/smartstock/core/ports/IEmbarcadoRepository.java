package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.Embarcado;
import br.com.smartstock.smartstock.core.enums.TipoEmbarcado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEmbarcadoRepository {
    Optional<Embarcado> findByChavePublica(String chavePublica);
    Optional<Embarcado> findByPublicId(UUID publicId);
    List<Embarcado> findByLojaId(Long lojaId);
    Long save(Embarcado embarcado);
    void update(Embarcado embarcado);
    void deleteByPublicId(UUID publicId);
    boolean existsByLojaIdAndTipo(Long lojaId, TipoEmbarcado tipo);
}
