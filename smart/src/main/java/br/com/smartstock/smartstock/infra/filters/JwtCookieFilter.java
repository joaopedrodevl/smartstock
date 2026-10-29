package br.com.smartstock.smartstock.infra.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
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

import static br.com.smartstock.smartstock.infra.utils.CookieUtil.extrairCookie;

@Component
public class JwtCookieFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    public JwtCookieFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extrairCookie(request, "access_token");

        if (token != null) {
            try {
                Jwt jwt = jwtDecoder.decode(token);
                String scope = jwt.getClaimAsString("scope");

                if (scope == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                var authorities = List.of(new SimpleGrantedAuthority(scope));
                var auth = new UsernamePasswordAuthenticationToken(jwt.getSubject(), jwt, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                // token inválido/expirado — segue sem autenticar, o Security Filter Chain barra depois
            }
        }

        filterChain.doFilter(request, response);
    }

}
