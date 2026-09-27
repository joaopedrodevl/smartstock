package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.application.exceptions.UserNotFoundException;
import br.com.smartstock.smartstock.core.entities.Admin;
import br.com.smartstock.smartstock.core.ports.IAdminRepository;
import br.com.smartstock.smartstock.core.valueobjects.Email;
import br.com.smartstock.smartstock.infra.api.dtos.LoginRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private IAdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private LoginRequestDTO loginRequest;
    private Admin admin;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequestDTO("admin@teste.com", "senha1234");

        admin = Admin.criar(
                br.com.smartstock.smartstock.core.valueobjects.Nome.of("Admin Teste"),
                br.com.smartstock.smartstock.core.valueobjects.Telefone.of("(11) 99999-8888"),
                Email.of("admin@teste.com"),
                "senhaHasheada"
        );
        admin.setId(1L);
    }

    @Test
    @DisplayName("Deve realizar login com sucesso")
    void deveRealizarLogin() {
        when(adminRepository.findByEmail("admin@teste.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("senha1234", "senhaHasheada")).thenReturn(true);
        when(jwtTokenService.generate("admin@teste.com", "ROLE_ADMIN")).thenReturn("access-token");
        when(refreshTokenService.gerar(1L, "Mozilla/5.0", "127.0.0.1")).thenReturn("refresh-token");

        AuthenticationService.AuthResult result = authenticationService.login(
                loginRequest, "Mozilla/5.0", "127.0.0.1"
        );

        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        verify(adminRepository, times(1)).atualizarUltimoLogin(1L);
    }

    @Test
    @DisplayName("Não deve login com email inexistente")
    void naoDeveLoginComEmailInexistente() {
        when(adminRepository.findByEmail("admin@teste.com")).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> authenticationService.login(loginRequest, "Mozilla/5.0", "127.0.0.1")
        );
    }

    @Test
    @DisplayName("Não deve login com senha inválida")
    void naoDeveLoginComSenhaInvalida() {
        when(adminRepository.findByEmail("admin@teste.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("senha1234", "senhaHasheada")).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login(loginRequest, "Mozilla/5.0", "127.0.0.1")
        );
        verify(adminRepository, never()).atualizarUltimoLogin(anyLong());
    }

    @Test
    @DisplayName("Deve realizar logout")
    void deveRealizarLogout() {
        authenticationService.logout("refresh-token-raw");
        verify(refreshTokenService, times(1)).revoke("refresh-token-raw");
    }

    @Test
    @DisplayName("Deve fazer logout mesmo com token nulo")
    void deveFazerLogoutComTokenNulo() {
        authenticationService.logout(null);
        verify(refreshTokenService, never()).revoke(anyString());
    }
}
