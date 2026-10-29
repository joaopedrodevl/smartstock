package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.RefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IRefreshTokenRepository {
    public void save(Long administradorId, String tokenHash, LocalDateTime expiraEm,
                     String userAgent, String ip);
    public Optional<RefreshToken> findByTokenHash(String tokenHash);
    public void revoke(String tokenHash);
}
