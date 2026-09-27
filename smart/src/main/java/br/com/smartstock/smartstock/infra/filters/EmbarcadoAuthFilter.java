package br.com.smartstock.smartstock.infra.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class EmbarcadoAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(EmbarcadoAuthFilter.class);

    private final JwtDecoder jwtDecoder;

    public EmbarcadoAuthFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = request.getHeader("X-Embarcado-Token");

        if (token != null && request.getRequestURI().startsWith("/v1/embarcado/")) {
            try {
                Jwt jwt = jwtDecoder.decode(token);
                String subject = jwt.getSubject(); // chavePublica
                String tipo = jwt.getClaimAsString("tipo");
                Long lojaId = ((Number) jwt.getClaim("lojaId")).longValue();
                String embarcadoId = jwt.getClaimAsString("embarcadoId");

                var authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_EMBARCADO"),
                        new SimpleGrantedAuthority("ROLE_" + tipo)
                );

                var auth = new UsernamePasswordAuthenticationToken(subject, jwt, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("Embarcado autenticado via token - chavePublica: {}, tipo: {}", subject, tipo);
            } catch (JwtException e) {
                log.debug("Token de embarcado invalido: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
