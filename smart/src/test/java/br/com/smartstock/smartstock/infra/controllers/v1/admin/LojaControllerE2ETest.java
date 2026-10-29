package br.com.smartstock.smartstock.infra.controllers.v1.admin;

import br.com.smartstock.smartstock.infra.api.dtos.AdminRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.LojaRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.EnderecoRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.LoginRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LojaControllerE2ETest {

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String accessToken;
    private static String lojaPublicId;

    @Test
    @Order(1)
    @DisplayName("Deve criar administrador e login")
    void deveCriarAdminELogin() throws Exception {
        String adminJson = objectMapper.writeValueAsString(
                new AdminRequestDTO(
                        "Admin Teste", "11999998888", "admin@smartstock.com", "123456"
                )
        );

        mockMvc.perform(post("/v1/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson)
                        .header("X-XSRF-TOKEN", "test-token"))
                .andExpect(status().isOk());

        String loginJson = objectMapper.writeValueAsString(
                new LoginRequestDTO("admin@smartstock.com", "123456")
        );

        MvcResult loginResult = mockMvc.perform(post("/v1/token/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        if (setCookieHeader != null && setCookieHeader.contains("access_token=")) {
            accessToken = setCookieHeader.split("access_token=")[1].split(";")[0];
        }
    }

    @Test
    @Order(2)
    @DisplayName("Deve criar loja com sucesso")
    void deveCriarLoja() throws Exception {
        LojaRequestDTO lojaRequest = new LojaRequestDTO(
                "11222333000181",
                "Mini Mercado Central",
                "Mini Mercado Central LTDA",
                "11999998888",
                true,
                new EnderecoRequestDTO(
                        "01310100",
                        "Avenida Paulista",
                        "1000",
                        "Sala 1",
                        "Bela Vista",
                        "São Paulo",
                        "SP",
                        "Em frente ao metrô"
                )
        );

        String lojaJson = objectMapper.writeValueAsString(lojaRequest);

        mockMvc.perform(post("/v1/admin/lojas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lojaJson)
                        .header("X-XSRF-TOKEN", "test-token")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("11222333000181"))
                .andExpect(jsonPath("$.nomeFantasia").value("Mini Mercado Central"))
                .andExpect(jsonPath("$.ehMatriz").value(true))
                .andExpect(jsonPath("$.endereco.cep").value("01310100"));
    }

    @Test
    @Order(3)
    @DisplayName("Não deve criar loja com CNPJ duplicado")
    void naoDeveCriarLojaComCnpjDuplicado() throws Exception {
        LojaRequestDTO lojaRequest = new LojaRequestDTO(
                "11222333000181",
                "Outra Loja",
                "Outra Loja LTDA",
                "11988887777",
                false,
                new EnderecoRequestDTO(
                        "04547000",
                        "Rua Flórida",
                        "500",
                        null,
                        "Vila Olímpia",
                        "São Paulo",
                        "SP",
                        null
                )
        );

        String lojaJson = objectMapper.writeValueAsString(lojaRequest);

        mockMvc.perform(post("/v1/admin/lojas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lojaJson)
                        .header("X-XSRF-TOKEN", "test-token")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("CNPJ já cadastrado"));
    }

    @Test
    @Order(4)
    @DisplayName("Deve listar lojas")
    void deveListarLojas() throws Exception {
        mockMvc.perform(get("/v1/admin/lojas")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cnpj").value("11222333000181"));
    }

    @Test
    @Order(5)
    @DisplayName("Deve buscar loja por publicId")
    void deveBuscarLojaPorPublicId() throws Exception {
        MvcResult result = mockMvc.perform(get("/v1/admin/lojas")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        lojaPublicId = objectMapper.readTree(response).get(0).get("publicId").asText();

        mockMvc.perform(get("/v1/admin/lojas/" + lojaPublicId)
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("11222333000181"));
    }

    @Test
    @Order(6)
    @DisplayName("Deve atualizar loja")
    void deveAtualizarLoja() throws Exception {
        LojaRequestDTO lojaRequest = new LojaRequestDTO(
                "11222333000181",
                "Mini Mercado Central Atualizado",
                "Mini Mercado Central LTDA",
                "11977776666",
                true,
                new EnderecoRequestDTO(
                        "01310100",
                        "Avenida Paulista",
                        "1500",
                        "Loja 2",
                        "Bela Vista",
                        "São Paulo",
                        "SP",
                        "Próximo ao hospital"
                )
        );

        String lojaJson = objectMapper.writeValueAsString(lojaRequest);

        mockMvc.perform(put("/v1/admin/lojas/" + lojaPublicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lojaJson)
                        .header("X-XSRF-TOKEN", "test-token")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeFantasia").value("Mini Mercado Central Atualizado"))
                .andExpect(jsonPath("$.telefone").value("11977776666"));
    }

    @Test
    @Order(7)
    @DisplayName("Deve desativar loja")
    void deveDesativarLoja() throws Exception {
        mockMvc.perform(delete("/v1/admin/lojas/" + lojaPublicId)
                        .header("X-XSRF-TOKEN", "test-token")
                        .cookie(new Cookie("access_token", accessToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(8)
    @DisplayName("Não deve acessar loja sem autenticação")
    void naoDeveAcessarLojaSemAutenticacao() throws Exception {
        mockMvc.perform(get("/v1/admin/lojas"))
                .andExpect(status().isUnauthorized());
    }
}
