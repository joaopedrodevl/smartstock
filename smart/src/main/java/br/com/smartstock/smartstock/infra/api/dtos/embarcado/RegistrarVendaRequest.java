package br.com.smartstock.smartstock.infra.api.dtos.embarcado;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RegistrarVendaRequest(
        @NotEmpty(message = "A venda deve ter pelo menos um item")
        List<String> codigosRfid
) {
}
