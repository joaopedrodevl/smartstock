package br.com.smartstock.smartstock.infra.api.dtos.produto;

import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtualizarProdutoRequest(
        @NotNull(message = "Nome e obrigatorio")
        @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
        String nome,

        @Size(max = 255, message = "Descricao deve ter no maximo 255 caracteres")
        String descricao,

        @Size(max = 50, message = "Codigo de barras deve ter no maximo 50 caracteres")
        String codigoBarras,

        @NotNull(message = "Preco e obrigatorio (em centavos)")
        Dinheiro preco,

        @NotNull(message = "Quantidade e obrigatoria")
        @Positive(message = "Quantidade deve ser positiva")
        Integer quantidade,

        LocalDate dataValidade
) {
}
