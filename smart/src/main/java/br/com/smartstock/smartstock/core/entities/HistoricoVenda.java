package br.com.smartstock.smartstock.core.entities;

import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HistoricoVenda {
    @EqualsAndHashCode.Include
    private Long id;
    private UUID publicId;
    private Long lojaId;
    private Long produtoId;
    private String codigoRfid;
    private Dinheiro precoUnitario;
    private LocalDateTime vendidoEm;

    public static HistoricoVenda criar(Long lojaId, Long produtoId, String codigoRfid, Dinheiro precoUnitario) {
        HistoricoVenda h = new HistoricoVenda();
        h.setPublicId(UUID.randomUUID());
        h.setLojaId(lojaId);
        h.setProdutoId(produtoId);
        h.setCodigoRfid(codigoRfid);
        h.setPrecoUnitario(precoUnitario);
        h.setVendidoEm(LocalDateTime.now());
        return h;
    }
}
