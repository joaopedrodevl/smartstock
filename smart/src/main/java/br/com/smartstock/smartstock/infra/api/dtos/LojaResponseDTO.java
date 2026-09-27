package br.com.smartstock.smartstock.infra.api.dtos;

import java.util.UUID;

public record LojaResponseDTO(
        UUID publicId,
        String cnpj,
        String nomeFantasia,
        String razaoSocial,
        String telefone,
        Boolean ehMatriz,
        Boolean ativo,
        EnderecoResponseDTO endereco
) {
}
