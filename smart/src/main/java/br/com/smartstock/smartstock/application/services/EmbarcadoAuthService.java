package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.core.entities.Embarcado;
import br.com.smartstock.smartstock.core.ports.IEmbarcadoRepository;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.AuthEmbarcadoResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class EmbarcadoAuthService {

    private static final Logger log = LoggerFactory.getLogger(EmbarcadoAuthService.class);
    private static final long TOKEN_EXPIRATION_MINUTES = 30;

    private final IEmbarcadoRepository embarcadoRepository;
    private final JwtEncoder jwtEncoder;

    public EmbarcadoAuthService(IEmbarcadoRepository embarcadoRepository, JwtEncoder jwtEncoder) {
        this.embarcadoRepository = embarcadoRepository;
        this.jwtEncoder = jwtEncoder;
    }

    public AuthEmbarcadoResponseDTO autenticar(String chavePublica) {
        log.info("Autenticando embarcado - chavePublica: {}", chavePublica);

        Embarcado embarcado = embarcadoRepository.findByChavePublica(chavePublica)
                .orElseThrow(() -> {
                    log.warn("Embarcado nao encontrado para autenticacao - chavePublica: {}", chavePublica);
                    return new IllegalArgumentException("Chave publica invalida");
                });

        if (!embarcado.getAtivo()) {
            log.warn("Tentativa de autenticacao com embarcado inativo - chavePublica: {}", chavePublica);
            throw new IllegalArgumentException("Embarcado inativo");
        }

        Instant now = Instant.now();
        Instant expiry = now.plus(TOKEN_EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("smartstock")
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(chavePublica)
                .claim("scope", "ROLE_EMBARCADO")
                .claim("tipo", embarcado.getTipo().name())
                .claim("lojaId", embarcado.getLojaId())
                .claim("embarcadoId", embarcado.getPublicId().toString())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        LocalDateTime expiraEm = LocalDateTime.ofInstant(expiry, ZoneId.systemDefault());

        log.info("Embarcado autenticado com sucesso - chavePublica: {}, tipo: {}", chavePublica, embarcado.getTipo());
        return new AuthEmbarcadoResponseDTO(token, expiraEm);
    }

    public Optional<Embarcado> validarToken(String token) {
        // O token e validado pelo JwtCookieFilter, aqui apenas buscamos o embarcado
        // Esta funcao e usada quando precisamos do objeto Embarcado completo
        return Optional.empty();
    }
}
