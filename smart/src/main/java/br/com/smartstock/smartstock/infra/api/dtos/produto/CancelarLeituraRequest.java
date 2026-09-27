package br.com.smartstock.smartstock.infra.api.dtos.produto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelarLeituraRequest(
        @NotNull(message = "Leitura public ID e obrigatorio")
        UUID leituraPublicId
) {
}
