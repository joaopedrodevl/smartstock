package br.com.smartstock.smartstock.core.entities;

import br.com.smartstock.smartstock.core.valueobjects.Email;
import br.com.smartstock.smartstock.core.valueobjects.Nome;
import br.com.smartstock.smartstock.core.valueobjects.Telefone;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Admin {
    @EqualsAndHashCode.Include
    private Long id;
    private UUID publicId;
    private Nome nome;
    private Telefone telefone;
    private Email email;
    private String senhaHash;
    private String role;
    private Long lojaId;
    private Boolean ativo;
    private LocalDateTime ultimoLogin;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static Admin criar(Nome nome, Telefone telefone, Email email, String senhaHash) {
        Admin admin = new Admin();
        admin.setPublicId(UUID.randomUUID());
        admin.setNome(nome);
        admin.setTelefone(telefone);
        admin.setEmail(email);
        admin.setSenhaHash(senhaHash);
        admin.setRole("ROLE_ADMIN");
        admin.setAtivo(true);
        return admin;
    }

    public static Admin criarVendaAdmin(Nome nome, Telefone telefone, Email email, String senhaHash, Long lojaId) {
        Admin admin = criar(nome, telefone, email, senhaHash);
        admin.setRole("ROLE_VENDA_ADMIN");
        admin.setLojaId(lojaId);
        return admin;
    }

    public void atualizarDados(Nome nome, Telefone telefone) {
        this.nome = nome;
        this.telefone = telefone;
    }

    public void atualizarSenha(String novaSenhaHash) {
        this.senhaHash = novaSenhaHash;
    }

    public void registrarLogin() {
        this.ultimoLogin = LocalDateTime.now();
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }
}
