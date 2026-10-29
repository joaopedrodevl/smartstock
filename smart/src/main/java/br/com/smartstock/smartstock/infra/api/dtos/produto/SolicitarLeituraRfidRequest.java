package br.com.smartstock.smartstock.infra.api.dtos.produto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SolicitarLeituraRfidRequest(
        @NotNull(message = "Produto public ID e obrigatorio")
        UUID produtoPublicId,

        @NotNull(message = "Embarcado key e obrigatoria")
        String embarcadoKey
) {
}
