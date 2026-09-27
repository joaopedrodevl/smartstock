package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.application.exceptions.UserNotFoundException;
import br.com.smartstock.smartstock.core.ports.IAdminRepository;
import br.com.smartstock.smartstock.infra.api.dtos.LoginRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final IAdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationService(IAdminRepository adminRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtTokenService jwtTokenService,
                                  RefreshTokenService refreshTokenService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResult login(LoginRequestDTO request, String userAgent, String ip) {
        log.info("Tentativa de login - email: {}, ip: {}", request.email(), ip);

        var admin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login falhou - administrador não encontrado: {}", request.email());
                    return new UserNotFoundException(request.email());
                });

        if (!passwordEncoder.matches(request.senha(), admin.getSenhaHash())) {
            log.warn("Login falhou - credenciais inválidas para email: {}", request.email());
            throw new BadCredentialsException("Credenciais inválidas");
        }

        String accessToken = jwtTokenService.generate(admin.getEmail().valor(), admin.getRole());
        String refreshToken = refreshTokenService.gerar(admin.getId(), userAgent, ip);

        adminRepository.atualizarUltimoLogin(admin.getId());

        log.info("Login realizado com sucesso - email: {}, role: {}", request.email(), admin.getRole());
        return new AuthResult(accessToken, refreshToken);
    }

    public AuthResult refresh(String refreshTokenRaw, String userAgent, String ip) {
        log.info("Renovando token - ip: {}", ip);

        var refreshToken = refreshTokenService.validar(refreshTokenRaw)
                .orElseThrow(() -> {
                    log.warn("Refresh token invalido ou expirado");
                    return new BadCredentialsException("Refresh token invalido ou expirado");
                });

        var admin = adminRepository.findById(refreshToken.getAdministradorId())
                .orElseThrow(() -> {
                    log.warn("Administrador nao encontrado para refresh - id: {}", refreshToken.getAdministradorId());
                    return new UserNotFoundException("Administrador nao encontrado");
                });

        refreshTokenService.revoke(refreshTokenRaw);
        String novoRefreshToken = refreshTokenService.gerar(admin.getId(), userAgent, ip);
        String novoAccessToken = jwtTokenService.generate(admin.getEmail().valor(), admin.getRole());

        log.info("Token renovado com sucesso - email: {}", admin.getEmail());
        return new AuthResult(novoAccessToken, novoRefreshToken);
    }

    public void logout(String refreshTokenRaw) {
        log.info("Realizando logout");
        if (refreshTokenRaw != null) {
            refreshTokenService.revoke(refreshTokenRaw);
            log.info("Logout realizado com sucesso");
        }
    }

    public record AuthResult(String accessToken, String refreshToken) {}
}
