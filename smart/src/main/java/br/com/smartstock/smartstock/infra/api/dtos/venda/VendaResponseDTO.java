package br.com.smartstock.smartstock.infra.api.dtos.venda;

import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VendaResponseDTO(
        UUID publicId,
        UUID lojaPublicId,
        Dinheiro valorTotal,
        String status,
        String formaPagamento,
        LocalDateTime criadoEm,
        List<ItemVendaResponseDTO> itens
) {
    public record ItemVendaResponseDTO(
            String codigoRfid,
            String nomeProduto,
            Dinheiro precoUnitario
    ) {
    }
}
