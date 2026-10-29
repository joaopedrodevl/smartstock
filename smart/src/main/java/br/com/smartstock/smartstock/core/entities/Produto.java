package br.com.smartstock.smartstock.core.entities;

import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Produto {
    @EqualsAndHashCode.Include
    private Long id;
    private UUID publicId;
    private Long lojaId;
    private String codigoBarras;
    private String nome;
    private String descricao;
    private Dinheiro preco;
    private Integer estoque;
    private Boolean ativo;
    private LocalDate dataValidade;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static Produto criar(Long lojaId, String codigoBarras, String nome, String descricao, Dinheiro preco) {
        Produto p = new Produto();
        p.setPublicId(UUID.randomUUID());
        p.setLojaId(lojaId);
        p.setCodigoBarras(codigoBarras);
        p.setNome(nome);
        p.setDescricao(descricao);
        p.setPreco(preco);
        p.setEstoque(0);
        p.setAtivo(true);
        p.setDataValidade(null);
        return p;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void incrementarEstoque(int quantidade) {
        this.estoque += quantidade;
    }

    public void decrementarEstoque(int quantidade) {
        this.estoque -= quantidade;
    }
}
