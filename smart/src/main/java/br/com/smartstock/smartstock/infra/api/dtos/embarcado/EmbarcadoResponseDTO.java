package br.com.smartstock.smartstock.infra.api.dtos.embarcado;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmbarcadoResponseDTO(
        UUID publicId,
        UUID lojaPublicId,
        String chavePublica,
        String tipo,
        String descricao,
        Boolean ativo,
        LocalDateTime criadoEm
) {
}
