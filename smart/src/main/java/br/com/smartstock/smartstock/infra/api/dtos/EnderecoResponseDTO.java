package br.com.smartstock.smartstock.infra.api.dtos;

import java.util.UUID;

public record EnderecoResponseDTO(
        UUID publicId,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        String referencia
) {
}
