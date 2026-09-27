package br.com.smartstock.smartstock.infra.api.controllers.v1.admin;

import br.com.smartstock.smartstock.application.services.AuthenticationService;
import br.com.smartstock.smartstock.application.services.CookieService;
import br.com.smartstock.smartstock.infra.api.dtos.LoginRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.TokenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/token")
@Tag(name = "Autenticação", description = "Login, refresh e logout de administradores")
public class TokenController {

    private static final Logger log = LoggerFactory.getLogger(TokenController.class);

    private final AuthenticationService authService;
    private final CookieService cookieService;

    public TokenController(AuthenticationService authService, CookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/admin")
    @Operation(summary = "Login do administrador", description = "Autentica o administrador e retorna tokens de acesso")
    public ResponseEntity<TokenResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        log.info("Requisição de login - email: {}, ip: {}", request.email(), httpRequest.getRemoteAddr());

        var result = authService.login(
                request,
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
        );

        cookieService.setAccessTokenCookie(httpResponse, result.accessToken());
        cookieService.setRefreshTokenCookie(httpResponse, result.refreshToken());

        return ResponseEntity.ok(new TokenResponseDTO(result.accessToken(), result.refreshToken()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token", description = "Renova o access token usando o refresh token")
    public ResponseEntity<TokenResponseDTO> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {
        log.info("Requisição de refresh token - ip: {}", httpRequest.getRemoteAddr());

        String rawRefreshToken = cookieService.extractRefreshToken(httpRequest);

        var result = authService.refresh(
                rawRefreshToken,
                httpRequest.getHeader("User-Agent"),
                httpRequest.getRemoteAddr()
        );

        cookieService.setAccessTokenCookie(httpResponse, result.accessToken());
        cookieService.setRefreshTokenCookie(httpResponse, result.refreshToken());

        return ResponseEntity.ok(new TokenResponseDTO(result.accessToken(), result.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoga os tokens e encerra a sessão")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {
        log.info("Requisição de logout - ip: {}", httpRequest.getRemoteAddr());

        String rawRefreshToken = cookieService.extractRefreshToken(httpRequest);
        authService.logout(rawRefreshToken);
        cookieService.clearCookies(httpResponse);

        return ResponseEntity.noContent().build();
    }
}
