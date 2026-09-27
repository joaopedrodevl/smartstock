package br.com.smartstock.smartstock.infra.api.dtos.venda;

import jakarta.validation.constraints.NotBlank;

public record SolicitarLeituraVendaRequest(
        @NotBlank(message = "Embarcado key e obrigatoria")
        String embarcadoKey
) {
}
