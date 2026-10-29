package br.com.smartstock.smartstock.infra.api.dtos.venda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConsultarClienteRequest(
        @NotBlank(message = "CPF e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 digitos")
        String cpf
) {
}
