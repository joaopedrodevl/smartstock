package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.core.entities.Endereco;
import br.com.smartstock.smartstock.core.entities.Loja;
import br.com.smartstock.smartstock.core.ports.IEnderecoRepository;
import br.com.smartstock.smartstock.core.ports.ILojaRepository;
import br.com.smartstock.smartstock.infra.api.dtos.EnderecoRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.LojaRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.LojaResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LojaServiceTest {

    @Mock
    private ILojaRepository lojaRepository;

    @Mock
    private IEnderecoRepository enderecoRepository;

    @InjectMocks
    private LojaService lojaService;

    private LojaRequestDTO lojaRequestDTO;
    private Loja loja;
    private Endereco endereco;
    private Long administradorId;

    @BeforeEach
    void setUp() {
        administradorId = 1L;

        lojaRequestDTO = new LojaRequestDTO(
                "11222333000181",
                "Mini Mercado Central",
                "Mini Mercado Central LTDA",
                "(11) 99999-8888",
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

        endereco = Endereco.builder()
                .cep("01310100")
                .logradouro("Avenida Paulista")
                .numero("1000")
                .complemento("Sala 1")
                .bairro("Bela Vista")
                .cidade("São Paulo")
                .estado("SP")
                .referencia("Em frente ao metrô")
                .build();
        endereco.setId(1L);

        loja = Loja.builder()
                .administradorId(administradorId)
                .enderecoId(1L)
                .cnpj("11222333000181")
                .nomeFantasia("Mini Mercado Central")
                .razaoSocial("Mini Mercado Central LTDA")
                .telefone("(11) 99999-8888")
                .ehMatriz(true)
                .build();
        loja.setId(1L);
    }

    @Test
    @DisplayName("Deve criar loja com sucesso")
    void deveCriarLoja() {
        when(lojaRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(enderecoRepository.save(any(Endereco.class))).thenReturn(1L);
        when(lojaRepository.save(any(Loja.class))).thenReturn(1L);
        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));
        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));

        LojaResponseDTO response = lojaService.criar(administradorId, lojaRequestDTO);

        assertNotNull(response);
        assertEquals("11222333000181", response.cnpj());
        assertEquals("Mini Mercado Central", response.nomeFantasia());
        assertTrue(response.ehMatriz());
        verify(lojaRepository, times(1)).save(any(Loja.class));
        verify(enderecoRepository, times(1)).save(any(Endereco.class));
    }

    @Test
    @DisplayName("Não deve criar loja com CNPJ duplicado")
    void naoDeveCriarLojaComCnpjDuplicado() {
        when(lojaRepository.existsByCnpj("11222333000181")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lojaService.criar(administradorId, lojaRequestDTO)
        );

        assertEquals("CNPJ já cadastrado", exception.getMessage());
        verify(lojaRepository, never()).save(any(Loja.class));
    }

    @Test
    @DisplayName("Não deve criar segunda loja matriz")
    void naoDeveCriarSegundaLojaMatriz() {
        when(lojaRepository.existsByCnpj("11222333000181")).thenReturn(false);
        when(lojaRepository.findByAdministradorId(administradorId)).thenReturn(List.of(loja));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lojaService.criar(administradorId, lojaRequestDTO)
        );

        assertEquals("Já existe uma loja matriz para este administrador", exception.getMessage());
        verify(lojaRepository, never()).save(any(Loja.class));
    }

    @Test
    @DisplayName("Deve listar lojas do administrador")
    void deveListarLojasDoAdministrador() {
        when(lojaRepository.findByAdministradorId(administradorId)).thenReturn(List.of(loja));
        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));

        List<LojaResponseDTO> lojas = lojaService.listarPorAdministrador(administradorId);

        assertNotNull(lojas);
        assertEquals(1, lojas.size());
        assertEquals("11222333000181", lojas.get(0).cnpj());
    }

    @Test
    @DisplayName("Deve buscar loja por publicId")
    void deveBuscarLojaPorPublicId() {
        when(lojaRepository.findByPublicId(loja.getPublicId())).thenReturn(Optional.of(loja));
        when(enderecoRepository.findById(1L)).thenReturn(Optional.of(endereco));

        LojaResponseDTO response = lojaService.buscarPorPublicId(loja.getPublicId(), administradorId);

        assertNotNull(response);
        assertEquals(loja.getPublicId(), response.publicId());
    }

    @Test
    @DisplayName("Não deve buscar loja de outro administrador")
    void naoDeveBuscarLojaDeOutroAdministrador() {
        Loja lojaDeOutroAdmin = Loja.builder()
                .administradorId(99L)
                .enderecoId(2L)
                .cnpj("11444777000161")
                .nomeFantasia("Outra Loja")
                .razaoSocial("Outra Loja LTDA")
                .telefone("(11) 88888-7777")
                .ehMatriz(false)
                .build();
        lojaDeOutroAdmin.setId(2L);

        when(lojaRepository.findByPublicId(lojaDeOutroAdmin.getPublicId()))
                .thenReturn(Optional.of(lojaDeOutroAdmin));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> lojaService.buscarPorPublicId(lojaDeOutroAdmin.getPublicId(), administradorId)
        );

        assertEquals("Loja não pertence a este administrador", exception.getMessage());
    }

    @Test
    @DisplayName("Deve desativar loja")
    void deveDesativarLoja() {
        when(lojaRepository.findByPublicId(loja.getPublicId())).thenReturn(Optional.of(loja));

        lojaService.desativarPorPublicId(loja.getPublicId(), administradorId);

        assertFalse(loja.getAtivo());
        verify(lojaRepository, times(1)).update(loja);
    }
}
