package br.com.smartstock.smartstock.infra.api.dtos.venda;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RegistrarVendaRequest(
        @NotNull(message = "Loja public ID e obrigatorio")
        UUID lojaPublicId,

        @NotNull(message = "Embarcado key e obrigatoria")
        String embarcadoKey,

        @NotEmpty(message = "A venda deve ter pelo menos um item")
        List<String> codigosRfid
) {
}
