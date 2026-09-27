package br.com.smartstock.smartstock.core.entities;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cliente {
    @EqualsAndHashCode.Include
    private Long id;
    private String cpf;
    private String telefone;
    private String nome;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static Cliente criar(String cpf, String telefone, String nome) {
        Cliente c = new Cliente();
        c.setCpf(cpf);
        c.setTelefone(telefone);
        c.setNome(nome);
        return c;
    }
}
