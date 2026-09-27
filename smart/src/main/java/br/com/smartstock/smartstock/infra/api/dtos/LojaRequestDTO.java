package br.com.smartstock.smartstock.infra.api.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LojaRequestDTO(
        @NotBlank(message = "CNPJ é obrigatório")
        @Pattern(regexp = "^[A-Za-z0-9]{14}$", message = "CNPJ deve ter 14 caracteres alfanuméricos")
        String cnpj,

        @NotBlank(message = "Nome Fantasia é obrigatório")
        @Size(max = 255, message = "Nome Fantasia deve ter no máximo 255 caracteres")
        String nomeFantasia,

        @NotBlank(message = "Razão Social é obrigatória")
        @Size(max = 255, message = "Razão Social deve ter no máximo 255 caracteres")
        String razaoSocial,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\(\\d{2}\\)\\s?\\d{4,5}-\\d{4}$", message = "Telefone inválido (formato: (XX) XXXXX-XXXX)")
        String telefone,

        @NotNull(message = "Indicador de matriz é obrigatório")
        Boolean ehMatriz,

        @Valid
        @NotNull(message = "Endereço é obrigatório")
        EnderecoRequestDTO endereco
) {
}
