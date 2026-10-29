package br.com.smartstock.smartstock.core.entities;

import br.com.smartstock.smartstock.core.valueobjects.CEP;
import br.com.smartstock.smartstock.core.valueobjects.UF;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Endereco {
    @EqualsAndHashCode.Include
    private Long id;
    private UUID publicId;
    private CEP cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private UF estado;
    private String referencia;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static EnderecoBuilder builder() {
        return new EnderecoBuilder();
    }

    public static class EnderecoBuilder {
        private CEP cep;
        private String logradouro;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private UF estado;
        private String referencia;

        private EnderecoBuilder() {}

        public EnderecoBuilder cep(CEP cep) {
            this.cep = cep;
            return this;
        }

        public EnderecoBuilder cep(String cep) {
            this.cep = CEP.of(cep);
            return this;
        }

        public EnderecoBuilder logradouro(String logradouro) {
            this.logradouro = logradouro;
            return this;
        }

        public EnderecoBuilder numero(String numero) {
            this.numero = numero;
            return this;
        }

        public EnderecoBuilder complemento(String complemento) {
            this.complemento = complemento;
            return this;
        }

        public EnderecoBuilder bairro(String bairro) {
            this.bairro = bairro;
            return this;
        }

        public EnderecoBuilder cidade(String cidade) {
            this.cidade = cidade;
            return this;
        }

        public EnderecoBuilder estado(UF estado) {
            this.estado = estado;
            return this;
        }

        public EnderecoBuilder estado(String estado) {
            this.estado = UF.of(estado);
            return this;
        }

        public EnderecoBuilder referencia(String referencia) {
            this.referencia = referencia;
            return this;
        }

        public Endereco build() {
            Endereco endereco = new Endereco();
            endereco.setPublicId(UUID.randomUUID());
            endereco.setCep(this.cep);
            endereco.setLogradouro(this.logradouro);
            endereco.setNumero(this.numero);
            endereco.setComplemento(this.complemento);
            endereco.setBairro(this.bairro);
            endereco.setCidade(this.cidade);
            endereco.setEstado(this.estado);
            endereco.setReferencia(this.referencia);
            return endereco;
        }
    }

    public void atualizarDados(CEP cep, String logradouro, String numero, String complemento,
                                String bairro, String cidade, UF estado, String referencia) {
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.referencia = referencia;
    }
}
