package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.LeituraRfid;
import br.com.smartstock.smartstock.core.enums.StatusLeituraRfid;

import java.util.Optional;
import java.util.UUID;

public interface ILeituraRfidRepository {
    Optional<LeituraRfid> findByPublicId(UUID publicId);
    Optional<LeituraRfid> findPendenteByEmbarcadoKey(String embarcadoKey);
    Long save(LeituraRfid leitura);
    void updateStatus(UUID publicId, StatusLeituraRfid status, String codigoRfid);
}
