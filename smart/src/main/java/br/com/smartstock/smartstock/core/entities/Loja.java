package br.com.smartstock.smartstock.core.entities;

import br.com.smartstock.smartstock.core.valueobjects.CNPJ;
import br.com.smartstock.smartstock.core.valueobjects.Nome;
import br.com.smartstock.smartstock.core.valueobjects.RazaoSocial;
import br.com.smartstock.smartstock.core.valueobjects.Telefone;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Loja {
    @EqualsAndHashCode.Include
    private Long id;
    private UUID publicId;
    private Long administradorId;
    private Long enderecoId;
    private CNPJ cnpj;
    private Nome nomeFantasia;
    private RazaoSocial razaoSocial;
    private Telefone telefone;
    private Boolean ehMatriz;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static LojaBuilder builder() {
        return new LojaBuilder();
    }

    public static class LojaBuilder {
        private Long administradorId;
        private Long enderecoId;
        private CNPJ cnpj;
        private Nome nomeFantasia;
        private RazaoSocial razaoSocial;
        private Telefone telefone;
        private Boolean ehMatriz = false;

        private LojaBuilder() {}

        public LojaBuilder administradorId(Long administradorId) {
            this.administradorId = administradorId;
            return this;
        }

        public LojaBuilder enderecoId(Long enderecoId) {
            this.enderecoId = enderecoId;
            return this;
        }

        public LojaBuilder cnpj(CNPJ cnpj) {
            this.cnpj = cnpj;
            return this;
        }

        public LojaBuilder cnpj(String cnpj) {
            this.cnpj = CNPJ.of(cnpj);
            return this;
        }

        public LojaBuilder nomeFantasia(Nome nomeFantasia) {
            this.nomeFantasia = nomeFantasia;
            return this;
        }

        public LojaBuilder nomeFantasia(String nomeFantasia) {
            this.nomeFantasia = Nome.of(nomeFantasia);
            return this;
        }

        public LojaBuilder razaoSocial(RazaoSocial razaoSocial) {
            this.razaoSocial = razaoSocial;
            return this;
        }

        public LojaBuilder razaoSocial(String razaoSocial) {
            this.razaoSocial = RazaoSocial.of(razaoSocial);
            return this;
        }

        public LojaBuilder telefone(Telefone telefone) {
            this.telefone = telefone;
            return this;
        }

        public LojaBuilder telefone(String telefone) {
            this.telefone = Telefone.of(telefone);
            return this;
        }

        public LojaBuilder ehMatriz(Boolean ehMatriz) {
            this.ehMatriz = ehMatriz;
            return this;
        }

        public Loja build() {
            Loja loja = new Loja();
            loja.setPublicId(UUID.randomUUID());
            loja.setAdministradorId(this.administradorId);
            loja.setEnderecoId(this.enderecoId);
            loja.setCnpj(this.cnpj);
            loja.setNomeFantasia(this.nomeFantasia);
            loja.setRazaoSocial(this.razaoSocial);
            loja.setTelefone(this.telefone);
            loja.setEhMatriz(this.ehMatriz);
            loja.setAtivo(true);
            return loja;
        }
    }

    public void atualizarDados(CNPJ cnpj, Nome nomeFantasia, RazaoSocial razaoSocial, Telefone telefone, Boolean ehMatriz) {
        this.cnpj = cnpj;
        this.nomeFantasia = nomeFantasia;
        this.razaoSocial = razaoSocial;
        this.telefone = telefone;
        this.ehMatriz = ehMatriz;
    }

    public void vincularEndereco(Long enderecoId) {
        this.enderecoId = enderecoId;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public boolean isMatriz() {
        return Boolean.TRUE.equals(this.ehMatriz);
    }
}
