package br.com.smartstock.smartstock.infra.api.dtos.produto;

import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProdutoResponseDTO(
        UUID publicId,
        UUID lojaPublicId,
        String codigoBarras,
        String nome,
        String descricao,
        Dinheiro preco,
        Integer estoque,
        Integer tagsVinculadas,
        Boolean ativo,
        LocalDate dataValidade,
        LocalDateTime criadoEm
) {
}
