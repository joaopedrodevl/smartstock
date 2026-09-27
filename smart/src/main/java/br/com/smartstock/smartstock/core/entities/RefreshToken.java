package br.com.smartstock.smartstock.core.entities;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RefreshToken {
    private Long id;
    private Long administradorId;
    private String tokenHash;
    private LocalDateTime expiraEm;
    private Boolean revogado;
    private String userAgent;
    private String ip;

}
