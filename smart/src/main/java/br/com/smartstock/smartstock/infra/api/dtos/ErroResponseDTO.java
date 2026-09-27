package br.com.smartstock.smartstock.infra.api.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponseDTO(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        String path,
        String trace
) {
}
