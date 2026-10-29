package br.com.smartstock.smartstock.core.entities;

import br.com.smartstock.smartstock.core.enums.TipoEmbarcado;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Embarcado {
    @EqualsAndHashCode.Include
    private Long id;
    private UUID publicId;
    private Long lojaId;
    private String chavePublica;
    private String hmacSecret;
    private TipoEmbarcado tipo;
    private String descricao;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static Embarcado criar(Long lojaId, TipoEmbarcado tipo, String descricao) {
        Embarcado embarcado = new Embarcado();
        embarcado.setPublicId(UUID.randomUUID());
        embarcado.setLojaId(lojaId);
        embarcado.setTipo(tipo);
        embarcado.setChavePublica(UUID.randomUUID().toString());
        embarcado.setHmacSecret(UUID.randomUUID().toString());
        embarcado.setDescricao(descricao);
        embarcado.setAtivo(true);
        return embarcado;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public boolean isVenda() {
        return this.tipo == TipoEmbarcado.VENDA;
    }

    public boolean isCadastro() {
        return this.tipo == TipoEmbarcado.CADASTRO;
    }
}
