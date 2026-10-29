package br.com.smartstock.smartstock.core.entities;

import br.com.smartstock.smartstock.core.enums.FormaPagamento;
import br.com.smartstock.smartstock.core.enums.StatusVenda;
import br.com.smartstock.smartstock.core.valueobjects.Dinheiro;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Venda {
    @EqualsAndHashCode.Include
    private Long id;
    private UUID publicId;
    private Long lojaId;
    private Long clienteId;
    private Dinheiro valorTotal;
    private FormaPagamento formaPagamento;
    private StatusVenda status;
    private String observacao;
    private LocalDateTime finalizadaEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public static Venda criar(Long lojaId, Long clienteId) {
        Venda v = new Venda();
        v.setPublicId(UUID.randomUUID());
        v.setLojaId(lojaId);
        v.setClienteId(clienteId);
        v.setValorTotal(Dinheiro.deCentavos(0));
        v.setFormaPagamento(FormaPagamento.pix);
        v.setStatus(StatusVenda.aberta);
        v.setCriadoEm(LocalDateTime.now());
        return v;
    }

    public void finalizar(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
        this.status = StatusVenda.finalizada;
        this.finalizadaEm = LocalDateTime.now();
    }

    public void cancelar() {
        this.status = StatusVenda.cancelada;
        this.finalizadaEm = LocalDateTime.now();
    }

    public void adicionarValor(Dinheiro valor) {
        this.valorTotal = this.valorTotal.somar(valor);
    }
}
