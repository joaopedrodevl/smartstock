package br.com.smartstock.smartstock.core.entities;

import br.com.smartstock.smartstock.core.enums.StatusLeituraRfid;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LeituraRfid {
    @EqualsAndHashCode.Include
    private Long id;
    private UUID publicId;
    private Long lojaId;
    private String embarcadoKey;
    private StatusLeituraRfid status;
    private String codigoRfid;
    private Long produtoId;
    private LocalDateTime criadoEm;
    private LocalDateTime expiraEm;

    public static LeituraRfid criar(Long lojaId, String embarcadoKey, int tempoExpiracaoSegundos) {
        LeituraRfid leitura = new LeituraRfid();
        leitura.setPublicId(UUID.randomUUID());
        leitura.setLojaId(lojaId);
        leitura.setEmbarcadoKey(embarcadoKey);
        leitura.setStatus(StatusLeituraRfid.PENDENTE);
        leitura.setExpiraEm(LocalDateTime.now().plusSeconds(tempoExpiracaoSegundos));
        return leitura;
    }

    public void marcarLido(String codigoRfid) {
        this.status = StatusLeituraRfid.LIDO;
        this.codigoRfid = codigoRfid;
    }

    public void marcarExpirado() {
        this.status = StatusLeituraRfid.EXPIRADO;
    }

    public void marcarCancelado() {
        this.status = StatusLeituraRfid.CANCELADO;
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.expiraEm);
    }
}
