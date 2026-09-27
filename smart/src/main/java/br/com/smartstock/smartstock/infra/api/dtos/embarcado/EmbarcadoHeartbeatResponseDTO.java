package br.com.smartstock.smartstock.infra.api.dtos.embarcado;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmbarcadoHeartbeatResponseDTO(
        String status,
        String embarcadoId,
        UUID lojaPublicId,
        LocalDateTime timestamp
) {
}
