package br.com.smartstock.smartstock.infra.api.dtos.venda;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PagamentoRequest(
        @NotNull(message = "Venda public ID e obrigatorio")
        UUID vendaPublicId,

        @NotNull(message = "Forma de pagamento e obrigatoria")
        String formaPagamento
) {
}
