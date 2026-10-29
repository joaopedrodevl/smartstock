package br.com.smartstock.smartstock.infra.api.dtos.embarcado;

import java.time.LocalDateTime;

public record AuthEmbarcadoResponseDTO(
        String tokenSessao,
        LocalDateTime expiraEm
) {
}
