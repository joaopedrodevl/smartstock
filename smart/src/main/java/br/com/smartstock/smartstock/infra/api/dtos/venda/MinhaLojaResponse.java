package br.com.smartstock.smartstock.infra.api.dtos.venda;

import java.util.UUID;

public record MinhaLojaResponse(
        UUID publicId,
        String nomeFantasia
) {
}
