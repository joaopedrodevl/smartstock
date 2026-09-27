package br.com.smartstock.smartstock.core.entities;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TagRfid {
    @EqualsAndHashCode.Include
    private Long id;
    private UUID publicId;
    private Long lojaId;
    private Long produtoId;
    private String codigoRfid;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static TagRfid criar(Long lojaId, Long produtoId, String codigoRfid) {
        TagRfid tag = new TagRfid();
        tag.setPublicId(UUID.randomUUID());
        tag.setLojaId(lojaId);
        tag.setProdutoId(produtoId);
        tag.setCodigoRfid(codigoRfid);
        tag.setAtivo(true);
        return tag;
    }

    public void desvincular() {
        this.ativo = false;
    }
}
