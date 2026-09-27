package br.com.smartstock.smartstock.infra.api.dtos.embarcado;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEmbarcadoRequest(
        @NotNull(message = "Loja public ID e obrigatorio")
        UUID lojaPublicId,

        @NotNull(message = "Tipo e obrigatorio")
        br.com.smartstock.smartstock.core.enums.TipoEmbarcado tipo,

        String descricao
) {
}
