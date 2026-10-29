package br.com.smartstock.smartstock.infra.api.dtos.venda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

public record CriarVendaRequest(
        @NotNull(message = "Loja public ID e obrigatorio")
        UUID lojaPublicId,

        @NotBlank(message = "CPF e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 digitos")
        String cpfCliente,

        String telefoneCliente,

        @NotEmpty(message = "A venda deve ter pelo menos um item")
        List<String> codigosRfid
) {
}
