package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.core.entities.RefreshToken;
import br.com.smartstock.smartstock.core.ports.IRefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

public class RefreshTokenService {

    private final IRefreshTokenRepository repository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(IRefreshTokenRepository repository) {
        this.repository = repository;
    }

    public String gerar(Long administradorId, String userAgent, String ip) {
        // token opaco aleatório (não é JWT)
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        String tokenPuro = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        String tokenHash = hash(tokenPuro);
        LocalDateTime expiraEm = LocalDateTime.now().plusDays(7);

        repository.save(administradorId, tokenHash, expiraEm, userAgent, ip);

        return tokenPuro; // esse é o valor devolvido ao cliente
    }

    public Optional<RefreshToken> validar(String tokenPuro) {
        String tokenHash = hash(tokenPuro);
        return repository.findByTokenHash(tokenHash)
                .filter(rt -> rt.getExpiraEm().isAfter(LocalDateTime.now()));
    }

    public void revoke(String tokenPuro) {
        repository.revoke(hash(tokenPuro));
    }

    private String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
