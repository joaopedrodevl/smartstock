package br.com.smartstock.smartstock.infra.api.dtos.produto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LeituraRfidResponseDTO(
        UUID leituraPublicId,
        String status,
        String codigoRfid,
        LocalDateTime expiraEm
) {
}
