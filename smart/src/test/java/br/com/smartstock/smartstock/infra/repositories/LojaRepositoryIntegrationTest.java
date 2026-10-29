package br.com.smartstock.smartstock.infra.repositories;

import br.com.smartstock.smartstock.core.entities.Admin;
import br.com.smartstock.smartstock.core.entities.Endereco;
import br.com.smartstock.smartstock.core.entities.Loja;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LojaRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("smartstock_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private LojaRepository lojaRepository;

    private Long adminId;
    private Long enderecoId;

    @BeforeEach
    void setup() {
        if (adminId == null) {
            Admin admin = Admin.criar(
                    br.com.smartstock.smartstock.core.valueobjects.Nome.of("Admin Teste"),
                    br.com.smartstock.smartstock.core.valueobjects.Telefone.of("(11) 99999-8888"),
                    br.com.smartstock.smartstock.core.valueobjects.Email.of("admin@teste.com"),
                    "senhaHash"
            );
            adminRepository.save(admin);
            adminId = adminRepository.findByEmail("admin@teste.com").orElseThrow().getId();
        }

        if (enderecoId == null) {
            Endereco endereco = Endereco.builder()
                    .cep("01310100")
                    .logradouro("Avenida Paulista")
                    .numero("1000")
                    .bairro("Bela Vista")
                    .cidade("São Paulo")
                    .estado("SP")
                    .build();
            enderecoRepository.save(endereco);
            enderecoId = enderecoRepository.findByPublicId(endereco.getPublicId()).orElseThrow().getId();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Deve salvar loja com sucesso")
    void deveSalvarLoja() {
        Loja loja = Loja.builder()
                .administradorId(adminId)
                .enderecoId(enderecoId)
                .cnpj("11222333000181")
                .nomeFantasia("Mini Mercado Central")
                .razaoSocial("Mini Mercado Central LTDA")
                .telefone("(11) 99999-8888")
                .ehMatriz(true)
                .build();

        Long id = lojaRepository.save(loja);

        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    @Order(2)
    @DisplayName("Deve buscar loja por publicId")
    void deveBuscarLojaPorPublicId() {
        Loja loja = lojaRepository.findByAdministradorId(adminId).stream()
                .filter(l -> l.getCnpj().valor().equals("11222333000181"))
                .findFirst()
                .orElseThrow();

        Optional<Loja> found = lojaRepository.findByPublicId(loja.getPublicId());

        assertTrue(found.isPresent());
        assertEquals("11222333000181", found.get().getCnpj().valor());
        assertEquals("Mini Mercado Central", found.get().getNomeFantasia().valor());
    }

    @Test
    @Order(3)
    @DisplayName("Deve listar lojas do administrador")
    void deveListarLojasDoAdministrador() {
        List<Loja> lojas = lojaRepository.findByAdministradorId(adminId);

        assertFalse(lojas.isEmpty());
        assertEquals(1, lojas.size());
    }

    @Test
    @Order(4)
    @DisplayName("Deve verificar CNPJ existente")
    void deveVerificarCnpjExistente() {
        assertTrue(lojaRepository.existsByCnpj("11222333000181"));
        assertFalse(lojaRepository.existsByCnpj("99999999999999"));
    }

    @Test
    @Order(5)
    @DisplayName("Deve atualizar loja")
    void deveAtualizarLoja() {
        Loja loja = lojaRepository.findByAdministradorId(adminId).stream()
                .filter(l -> l.getCnpj().valor().equals("11222333000181"))
                .findFirst()
                .orElseThrow();

        loja.atualizarDados(
                br.com.smartstock.smartstock.core.valueobjects.CNPJ.of("11222333000181"),
                br.com.smartstock.smartstock.core.valueobjects.Nome.of("Mini Mercado Central Atualizado"),
                br.com.smartstock.smartstock.core.valueobjects.RazaoSocial.of("Mini Mercado Central Atualizado LTDA"),
                br.com.smartstock.smartstock.core.valueobjects.Telefone.of("(11) 88888-7777"),
                true
        );
        lojaRepository.update(loja);

        Loja updated = lojaRepository.findById(loja.getId()).orElseThrow();
        assertEquals("Mini Mercado Central Atualizado", updated.getNomeFantasia().valor());
    }

    @Test
    @Order(6)
    @DisplayName("Deve desativar loja")
    void deveDesativarLoja() {
        Loja loja = lojaRepository.findByAdministradorId(adminId).stream()
                .filter(l -> l.getCnpj().valor().equals("11222333000181"))
                .findFirst()
                .orElseThrow();

        loja.desativar();
        lojaRepository.update(loja);

        Loja updated = lojaRepository.findById(loja.getId()).orElseThrow();
        assertFalse(updated.getAtivo());
    }
}
