package br.com.smartstock.smartstock.infra.api.dtos.embarcado;

import jakarta.validation.constraints.NotBlank;

public record CadastrarProdutoRequest(
        @NotBlank(message = "RFID e obrigatorio")
        String rfid
) {
}
