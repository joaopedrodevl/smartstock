package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.application.exceptions.ResourceNotFoundException;
import br.com.smartstock.smartstock.core.entities.Embarcado;
import br.com.smartstock.smartstock.core.enums.TipoEmbarcado;
import br.com.smartstock.smartstock.core.ports.IEmbarcadoRepository;
import br.com.smartstock.smartstock.core.ports.ILojaRepository;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.CadastrarProdutoRequest;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.EmbarcadoHeartbeatResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.EmbarcadoResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.RegistrarVendaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EmbarcadoService {

    private static final Logger log = LoggerFactory.getLogger(EmbarcadoService.class);

    private final IEmbarcadoRepository embarcadoRepository;
    private final ILojaRepository lojaRepository;
    private final VendaService vendaService;

    public EmbarcadoService(IEmbarcadoRepository embarcadoRepository, ILojaRepository lojaRepository, VendaService vendaService) {
        this.embarcadoRepository = embarcadoRepository;
        this.lojaRepository = lojaRepository;
        this.vendaService = vendaService;
    }

    @Transactional
    public EmbarcadoResponseDTO cadastrar(UUID lojaPublicId, TipoEmbarcado tipo, String descricao) {
        log.info("Cadastrando embarcado - lojaPublicId: {}, tipo: {}", lojaPublicId, tipo);

        var loja = lojaRepository.findByPublicId(lojaPublicId)
                .orElseThrow(() -> {
                    log.warn("Loja nao encontrada - lojaPublicId: {}", lojaPublicId);
                    return new ResourceNotFoundException("Loja", lojaPublicId.toString());
                });

        if (embarcadoRepository.existsByLojaIdAndTipo(loja.getId(), tipo)) {
            log.warn("Ja existe embarcado do tipo {} para a loja {}", tipo, lojaPublicId);
            throw new IllegalArgumentException("Ja existe embarcado deste tipo para esta loja");
        }

        Embarcado embarcado = Embarcado.criar(loja.getId(), tipo, descricao);
        Long id = embarcadoRepository.save(embarcado);
        embarcado.setId(id);

        log.info("Embarcado criado com sucesso - publicId: {}, tipo: {}", embarcado.getPublicId(), tipo);
        return toResponse(embarcado);
    }

    public List<EmbarcadoResponseDTO> listarPorLoja(UUID lojaPublicId) {
        log.info("Listando embarcados da loja - lojaPublicId: {}", lojaPublicId);

        var loja = lojaRepository.findByPublicId(lojaPublicId)
                .orElseThrow(() -> {
                    log.warn("Loja nao encontrada - lojaPublicId: {}", lojaPublicId);
                    return new ResourceNotFoundException("Loja", lojaPublicId.toString());
                });

        return embarcadoRepository.findByLojaId(loja.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EmbarcadoResponseDTO buscarPorPublicId(UUID publicId) {
        log.info("Buscando embarcado - publicId: {}", publicId);
        Embarcado embarcado = embarcadoRepository.findByPublicId(publicId)
                .orElseThrow(() -> {
                    log.warn("Embarcado nao encontrado - publicId: {}", publicId);
                    return new IllegalArgumentException("Embarcado nao encontrado");
                });
        return toResponse(embarcado);
    }

    @Transactional
    public void remover(UUID publicId) {
        log.info("Removendo embarcado - publicId: {}", publicId);
        Embarcado embarcado = embarcadoRepository.findByPublicId(publicId)
                .orElseThrow(() -> {
                    log.warn("Embarcado nao encontrado para remocao - publicId: {}", publicId);
                    return new IllegalArgumentException("Embarcado nao encontrado");
                });
        embarcadoRepository.deleteByPublicId(publicId);
        log.info("Embarcado removido com sucesso - publicId: {}", publicId);
    }

    public EmbarcadoHeartbeatResponseDTO heartbeat(String chavePublica) {
        log.debug("Heartbeat recebido - chavePublica: {}", chavePublica);
        Embarcado embarcado = embarcadoRepository.findByChavePublica(chavePublica)
                .orElseThrow(() -> new IllegalArgumentException("Embarcado nao encontrado"));

        if (!embarcado.getAtivo()) {
            throw new IllegalArgumentException("Embarcado inativo");
        }

        var loja = lojaRepository.findById(embarcado.getLojaId())
                .orElseThrow(() -> new IllegalArgumentException("Loja nao encontrada"));

        return new EmbarcadoHeartbeatResponseDTO(
                "ok",
                embarcado.getPublicId().toString(),
                loja.getPublicId(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public void registrarVenda(String chavePublica, RegistrarVendaRequest request) {
        log.info("Registrando venda - chavePublica: {}, codigosRfid: {}", chavePublica, request.codigosRfid().size());
        Embarcado embarcado = embarcadoRepository.findByChavePublica(chavePublica)
                .orElseThrow(() -> new IllegalArgumentException("Embarcado nao encontrado"));

        if (!embarcado.getAtivo()) {
            throw new IllegalArgumentException("Embarcado inativo");
        }

        if (!embarcado.isVenda()) {
            log.warn("Tentativa de registrar venda com embarcado de cadastro - chavePublica: {}", chavePublica);
            throw new IllegalArgumentException("Embarcado nao e do tipo VENDA");
        }

        var loja = lojaRepository.findById(embarcado.getLojaId())
                .orElseThrow(() -> new IllegalArgumentException("Loja nao encontrada"));

        var vendaRequest = new br.com.smartstock.smartstock.infra.api.dtos.venda.RegistrarVendaRequest(
                loja.getPublicId(),
                chavePublica,
                request.codigosRfid()
        );

        vendaService.registrarVenda(vendaRequest);
        log.info("Venda registrada com sucesso");
    }

    @Transactional
    public void cadastrarProduto(String chavePublica, CadastrarProdutoRequest request) {
        log.info("Cadastrando produto via embarcado - chavePublica: {}, rfid: {}", chavePublica, request.rfid());
        Embarcado embarcado = embarcadoRepository.findByChavePublica(chavePublica)
                .orElseThrow(() -> new IllegalArgumentException("Embarcado nao encontrado"));

        if (!embarcado.getAtivo()) {
            throw new IllegalArgumentException("Embarcado inativo");
        }

        if (!embarcado.isCadastro()) {
            log.warn("Tentativa de cadastrar produto com embarcado de venda - chavePublica: {}", chavePublica);
            throw new IllegalArgumentException("Embarcado nao e do tipo CADASTRO");
        }

        log.info("RFID recebido do embarcado de cadastro - rfid: {}", request.rfid());
    }

    private EmbarcadoResponseDTO toResponse(Embarcado embarcado) {
        var loja = lojaRepository.findById(embarcado.getLojaId())
                .orElseThrow(() -> new IllegalArgumentException("Loja nao encontrada"));

        return new EmbarcadoResponseDTO(
                embarcado.getPublicId(),
                loja.getPublicId(),
                embarcado.getChavePublica(),
                embarcado.getTipo().name(),
                embarcado.getDescricao(),
                embarcado.getAtivo(),
                embarcado.getCriadoEm()
        );
    }
}
