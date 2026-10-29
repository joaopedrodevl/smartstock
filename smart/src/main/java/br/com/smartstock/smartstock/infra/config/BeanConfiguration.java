package br.com.smartstock.smartstock.infra.config;

import br.com.smartstock.smartstock.application.services.AuthenticationService;
import br.com.smartstock.smartstock.application.services.JwtTokenService;
import br.com.smartstock.smartstock.application.services.RefreshTokenService;
import br.com.smartstock.smartstock.core.ports.IAdminRepository;
import br.com.smartstock.smartstock.core.ports.IRefreshTokenRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

@Configuration
public class BeanConfiguration {

    @Bean
    public RefreshTokenService refreshTokenService(IRefreshTokenRepository repository) {
        return new RefreshTokenService(repository);
    }

    @Bean
    public JwtTokenService jwtTokenService(JwtEncoder jwtEncoder) {
        return new JwtTokenService(jwtEncoder);
    }

    @Bean
    public AuthenticationService authenticationService(
            IAdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService) {
        return new AuthenticationService(adminRepository, passwordEncoder, jwtTokenService, refreshTokenService);
    }
}
