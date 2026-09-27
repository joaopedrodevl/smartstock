package br.com.smartstock.smartstock.infra.api.dtos.embarcado;

import jakarta.validation.constraints.NotBlank;

public record AuthEmbarcadoRequest(
        @NotBlank(message = "Chave publica e obrigatoria")
        String chavePublica
) {
}
