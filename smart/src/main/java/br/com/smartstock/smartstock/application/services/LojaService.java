package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.core.entities.Endereco;
import br.com.smartstock.smartstock.core.entities.Loja;
import br.com.smartstock.smartstock.core.ports.IEnderecoRepository;
import br.com.smartstock.smartstock.core.ports.ILojaRepository;
import br.com.smartstock.smartstock.core.valueobjects.CNPJ;
import br.com.smartstock.smartstock.core.valueobjects.CEP;
import br.com.smartstock.smartstock.core.valueobjects.Nome;
import br.com.smartstock.smartstock.core.valueobjects.RazaoSocial;
import br.com.smartstock.smartstock.core.valueobjects.Telefone;
import br.com.smartstock.smartstock.core.valueobjects.UF;
import br.com.smartstock.smartstock.infra.api.dtos.EnderecoResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.LojaRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.LojaResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.PagedResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LojaService {

    private static final Logger log = LoggerFactory.getLogger(LojaService.class);

    private final ILojaRepository lojaRepository;
    private final IEnderecoRepository enderecoRepository;

    public LojaService(ILojaRepository lojaRepository, IEnderecoRepository enderecoRepository) {
        this.lojaRepository = lojaRepository;
        this.enderecoRepository = enderecoRepository;
    }

    @Transactional
    public LojaResponseDTO criar(Long administradorId, LojaRequestDTO dto) {
        log.info("Criando loja - adminId: {}, cnpj: {}, nomeFantasia: {}", administradorId, dto.cnpj(), dto.nomeFantasia());

        if (lojaRepository.existsByCnpj(dto.cnpj())) {
            log.warn("Tentativa de criar loja com CNPJ já cadastrado: {}", dto.cnpj());
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }

        if (dto.ehMatriz() != null && dto.ehMatriz()) {
            boolean jaTemMatriz = lojaRepository.findByAdministradorId(administradorId)
                    .stream()
                    .anyMatch(Loja::isMatriz);
            if (jaTemMatriz) {
                log.warn("Tentativa de criar segunda loja matriz para adminId: {}", administradorId);
                throw new IllegalArgumentException("Já existe uma loja matriz para este administrador");
            }
        }

        Endereco endereco = Endereco.builder()
                .cep(dto.endereco().cep())
                .logradouro(dto.endereco().logradouro())
                .numero(dto.endereco().numero())
                .complemento(dto.endereco().complemento())
                .bairro(dto.endereco().bairro())
                .cidade(dto.endereco().cidade())
                .estado(dto.endereco().estado())
                .referencia(dto.endereco().referencia())
                .build();

        Long enderecoId = enderecoRepository.save(endereco);
        log.debug("Endereço salvo - id: {}", enderecoId);

        Loja loja = Loja.builder()
                .administradorId(administradorId)
                .enderecoId(enderecoId)
                .cnpj(dto.cnpj())
                .nomeFantasia(dto.nomeFantasia())
                .razaoSocial(dto.razaoSocial())
                .telefone(dto.telefone())
                .ehMatriz(dto.ehMatriz() != null ? dto.ehMatriz() : false)
                .build();

        Long lojaId = lojaRepository.save(loja);

        Endereco enderecoSalvo = enderecoRepository.findById(enderecoId).orElseThrow();

        LojaResponseDTO response = toResponse(lojaRepository.findById(lojaId).orElseThrow(), enderecoSalvo);
        log.info("Loja criada com sucesso - publicId: {}", response.publicId());
        return response;
    }

    public List<LojaResponseDTO> listarPorAdministrador(Long administradorId) {
        log.info("Listando lojas do adminId: {}", administradorId);
        List<LojaResponseDTO> lojas = lojaRepository.findByAdministradorId(administradorId)
                .stream()
                .map(loja -> {
                    Endereco endereco = enderecoRepository.findById(loja.getEnderecoId()).orElseThrow();
                    return toResponse(loja, endereco);
                })
                .toList();
        log.debug("Encontradas {} lojas para adminId: {}", lojas.size(), administradorId);
        return lojas;
    }

    public PagedResponseDTO<LojaResponseDTO> listarPorAdministrador(Long administradorId, int page, int size) {
        log.info("Listando lojas do adminId: {} - page: {}, size: {}", administradorId, page, size);
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        long totalElements = lojaRepository.countByAdministradorId(administradorId);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<LojaResponseDTO> lojas = lojaRepository.findByAdministradorId(administradorId, page, size)
                .stream()
                .map(loja -> {
                    Endereco endereco = enderecoRepository.findById(loja.getEnderecoId()).orElseThrow();
                    return toResponse(loja, endereco);
                })
                .toList();

        log.debug("Encontradas {} lojas (total: {}) para adminId: {}", lojas.size(), totalElements, administradorId);
        return new PagedResponseDTO<>(lojas, page, size, totalElements, totalPages, page == 0, page >= totalPages - 1);
    }

    public LojaResponseDTO buscarPorPublicId(UUID publicId, Long administradorId) {
        log.info("Buscando loja - publicId: {}, adminId: {}", publicId, administradorId);
        Loja loja = lojaRepository.findByPublicId(publicId)
                .orElseThrow(() -> {
                    log.warn("Loja não encontrada - publicId: {}", publicId);
                    return new IllegalArgumentException("Loja não encontrada");
                });

        if (!loja.getAdministradorId().equals(administradorId)) {
            log.warn("Tentativa de acessar loja de outro admin - lojaPublicId: {}, adminId: {}", publicId, administradorId);
            throw new IllegalArgumentException("Loja não pertence a este administrador");
        }

        Endereco endereco = enderecoRepository.findById(loja.getEnderecoId()).orElseThrow();
        return toResponse(loja, endereco);
    }

    @Transactional
    public LojaResponseDTO atualizarPorPublicId(UUID publicId, Long administradorId, LojaRequestDTO dto) {
        log.info("Atualizando loja - publicId: {}, adminId: {}", publicId, administradorId);
        Loja loja = lojaRepository.findByPublicId(publicId)
                .orElseThrow(() -> {
                    log.warn("Loja não encontrada para atualização - publicId: {}", publicId);
                    return new IllegalArgumentException("Loja não encontrada");
                });

        if (!loja.getAdministradorId().equals(administradorId)) {
            log.warn("Tentativa de atualizar loja de outro admin - lojaPublicId: {}, adminId: {}", publicId, administradorId);
            throw new IllegalArgumentException("Loja não pertence a este administrador");
        }

        if (lojaRepository.existsByCnpjAndIdNot(dto.cnpj(), loja.getId())) {
            log.warn("Tentativa de atualizar loja com CNPJ duplicado - cnpj: {}", dto.cnpj());
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }

        if (dto.ehMatriz() != null && dto.ehMatriz() && !loja.isMatriz()) {
            boolean jaTemMatriz = lojaRepository.findByAdministradorId(administradorId)
                    .stream()
                    .anyMatch(l -> l.isMatriz() && !l.getId().equals(loja.getId()));
            if (jaTemMatriz) {
                log.warn("Tentativa de definir loja como matriz já existente - adminId: {}", administradorId);
                throw new IllegalArgumentException("Já existe outra loja matriz para este administrador");
            }
        }

        Endereco endereco = enderecoRepository.findById(loja.getEnderecoId()).orElseThrow();
        endereco.atualizarDados(
                CEP.of(dto.endereco().cep()),
                dto.endereco().logradouro(),
                dto.endereco().numero(),
                dto.endereco().complemento(),
                dto.endereco().bairro(),
                dto.endereco().cidade(),
                UF.of(dto.endereco().estado()),
                dto.endereco().referencia()
        );
        enderecoRepository.update(endereco);

        loja.atualizarDados(
                CNPJ.of(dto.cnpj()),
                Nome.of(dto.nomeFantasia()),
                RazaoSocial.of(dto.razaoSocial()),
                Telefone.of(dto.telefone()),
                dto.ehMatriz() != null ? dto.ehMatriz() : false
        );
        lojaRepository.update(loja);

        log.info("Loja atualizada com sucesso - publicId: {}", publicId);
        return toResponse(loja, endereco);
    }

    public void desativarPorPublicId(UUID publicId, Long administradorId) {
        log.info("Desativando loja - publicId: {}, adminId: {}", publicId, administradorId);
        Loja loja = lojaRepository.findByPublicId(publicId)
                .orElseThrow(() -> {
                    log.warn("Loja não encontrada para desativação - publicId: {}", publicId);
                    return new IllegalArgumentException("Loja não encontrada");
                });

        if (!loja.getAdministradorId().equals(administradorId)) {
            log.warn("Tentativa de desativar loja de outro admin - lojaPublicId: {}, adminId: {}", publicId, administradorId);
            throw new IllegalArgumentException("Loja não pertence a este administrador");
        }

        loja.desativar();
        lojaRepository.update(loja);
        log.info("Loja desativada com sucesso - publicId: {}", publicId);
    }

    private LojaResponseDTO toResponse(Loja loja, Endereco endereco) {
        EnderecoResponseDTO enderecoResponse = new EnderecoResponseDTO(
                endereco.getPublicId(),
                endereco.getCep().valor(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getEstado().valor(),
                endereco.getReferencia()
        );

        return new LojaResponseDTO(
                loja.getPublicId(),
                loja.getCnpj().valor(),
                loja.getNomeFantasia().valor(),
                loja.getRazaoSocial().valor(),
                loja.getTelefone().valor(),
                loja.getEhMatriz(),
                loja.getAtivo(),
                enderecoResponse
        );
    }
}
