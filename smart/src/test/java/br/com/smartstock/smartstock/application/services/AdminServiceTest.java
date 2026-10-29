package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.core.entities.Admin;
import br.com.smartstock.smartstock.core.ports.IAdminRepository;
import br.com.smartstock.smartstock.core.valueobjects.Email;
import br.com.smartstock.smartstock.infra.api.dtos.AdminRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private IAdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    private AdminRequestDTO adminRequestDTO;

    @BeforeEach
    void setUp() {
        adminRequestDTO = new AdminRequestDTO(
                "Admin Teste",
                "(11) 99999-8888",
                "admin@teste.com",
                "senha1234"
        );
    }

    @Test
    @DisplayName("Deve criar administrador com sucesso")
    void deveCriarAdministrador() {
        when(adminRepository.findByEmail("admin@teste.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha1234")).thenReturn("senhaHasheada");

        Admin admin = adminService.criar(adminRequestDTO);

        assertNotNull(admin);
        assertEquals("Admin Teste", admin.getNome().valor());
        assertEquals("admin@teste.com", admin.getEmail().valor());
        assertTrue(admin.getAtivo());
        assertNotNull(admin.getPublicId());
        verify(adminRepository, times(1)).save(any(Admin.class));
    }

    @Test
    @DisplayName("Não deve criar administrador com email duplicado")
    void naoDeveCriarAdministradorComEmailDuplicado() {
        Admin adminExistente = Admin.criar(
                br.com.smartstock.smartstock.core.valueobjects.Nome.of("Admin Existente"),
                br.com.smartstock.smartstock.core.valueobjects.Telefone.of("(11) 88888-7777"),
                Email.of("admin@teste.com"),
                "senhaHasheada"
        );

        when(adminRepository.findByEmail("admin@teste.com")).thenReturn(Optional.of(adminExistente));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.criar(adminRequestDTO)
        );

        assertEquals("Email já cadastrado", exception.getMessage());
        verify(adminRepository, never()).save(any(Admin.class));
    }
}
