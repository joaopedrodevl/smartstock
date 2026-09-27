package br.com.smartstock.smartstock.infra.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequestDTO(
        @NotBlank(message = "CEP é obrigatório")
        @Pattern(regexp = "^\\d{8}$", message = "CEP deve ter 8 dígitos")
        String cep,

        @NotBlank(message = "Logradouro é obrigatório")
        @Size(max = 255, message = "Logradouro deve ter no máximo 255 caracteres")
        String logradouro,

        @NotBlank(message = "Número é obrigatório")
        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres")
        String numero,

        @Size(max = 255, message = "Complemento deve ter no máximo 255 caracteres")
        String complemento,

        @NotBlank(message = "Bairro é obrigatório")
        @Size(max = 255, message = "Bairro deve ter no máximo 255 caracteres")
        String bairro,

        @NotBlank(message = "Cidade é obrigatória")
        @Size(max = 255, message = "Cidade deve ter no máximo 255 caracteres")
        String cidade,

        @NotBlank(message = "Estado é obrigatório")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Estado deve ter 2 letras maiúsculas (UF)")
        String estado,

        @Size(max = 255, message = "Referência deve ter no máximo 255 caracteres")
        String referencia
) {
}
