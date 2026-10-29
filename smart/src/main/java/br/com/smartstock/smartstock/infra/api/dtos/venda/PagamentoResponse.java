package br.com.smartstock.smartstock.infra.api.dtos.venda;

import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;

import java.time.LocalDateTime;
import java.util.UUID;

public record PagamentoResponse(
        UUID vendaPublicId,
        String status,
        String formaPagamento,
        Dinheiro valorTotal,
        LocalDateTime processadoEm
) {
}
