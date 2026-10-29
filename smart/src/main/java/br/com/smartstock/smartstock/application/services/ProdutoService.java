package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.application.exceptions.ResourceNotFoundException;
import br.com.smartstock.smartstock.core.entities.LeituraRfid;
import br.com.smartstock.smartstock.core.entities.Produto;
import br.com.smartstock.smartstock.core.entities.TagRfid;
import br.com.smartstock.smartstock.core.enums.StatusLeituraRfid;
import br.com.smartstock.smartstock.core.ports.*;
import br.com.smartstock.smartstock.infra.api.dtos.produto.*;
import br.com.smartstock.smartstock.infra.websocket.EmbarcadoWebSocketHandler;
import br.com.smartstock.smartstock.infra.websocket.LeituraRfidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class ProdutoService {

    private static final Logger log = LoggerFactory.getLogger(ProdutoService.class);
    private static final int TEMPO_EXPIRACAO_LEITURA = 30;

    private final IProdutoRepository produtoRepository;
    private final ILojaRepository lojaRepository;
    private final ITagRfidRepository tagRfidRepository;
    private final ILeituraRfidRepository leituraRfidRepository;
    private final EmbarcadoWebSocketHandler webSocketHandler;

    private final ConcurrentHashMap<UUID, CompletableFuture<String>> leiturasPendentes = new ConcurrentHashMap<>();

    public ProdutoService(IProdutoRepository produtoRepository, ILojaRepository lojaRepository,
                          ITagRfidRepository tagRfidRepository, ILeituraRfidRepository leituraRfidRepository,
                          EmbarcadoWebSocketHandler webSocketHandler) {
        this.produtoRepository = produtoRepository;
        this.lojaRepository = lojaRepository;
        this.tagRfidRepository = tagRfidRepository;
        this.leituraRfidRepository = leituraRfidRepository;
        this.webSocketHandler = webSocketHandler;
    }

    @Transactional
    public ProdutoResponseDTO criar(CriarProdutoRequest dto) {
        log.info("Criando produto - lojaPublicId: {}, nome: {}, quantidade: {}", dto.lojaPublicId(), dto.nome(), dto.quantidade());

        if (dto.preco().getCentavos() <= 0) {
            throw new IllegalArgumentException("Preco deve ser maior que zero");
        }

        var loja = lojaRepository.findByPublicId(dto.lojaPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Loja", dto.lojaPublicId().toString()));

        Produto produto = Produto.criar(loja.getId(), dto.codigoBarras(), dto.nome(), dto.descricao(), dto.preco());
        produto.setEstoque(dto.quantidade());
        produto.setDataValidade(dto.dataValidade());
        Long id = produtoRepository.save(produto);
        produto.setId(id);

        log.info("Produto criado com sucesso - publicId: {}, estoque: {}", produto.getPublicId(), produto.getEstoque());
        return toResponse(produto);
    }

    public List<ProdutoResponseDTO> listarPorLoja(UUID lojaPublicId) {
        log.info("Listando produtos da loja - lojaPublicId: {}", lojaPublicId);

        var loja = lojaRepository.findByPublicId(lojaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Loja", lojaPublicId.toString()));

        return produtoRepository.findByLojaId(loja.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProdutoResponseDTO buscarPorPublicId(UUID publicId) {
        log.info("Buscando produto - publicId: {}", publicId);
        Produto produto = produtoRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", publicId.toString()));
        return toResponse(produto);
    }

    @Transactional
    public ProdutoResponseDTO atualizar(UUID publicId, AtualizarProdutoRequest dto) {
        log.info("Atualizando produto - publicId: {}, nome: {}", publicId, dto.nome());

        if (dto.preco().getCentavos() <= 0) {
            throw new IllegalArgumentException("Preco deve ser maior que zero");
        }

        Produto produto = produtoRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", publicId.toString()));

        produto.setNome(dto.nome());
        produto.setDescricao(dto.descricao());
        produto.setCodigoBarras(dto.codigoBarras());
        produto.setPreco(dto.preco());
        produto.setEstoque(dto.quantidade());
        produto.setDataValidade(dto.dataValidade());

        produtoRepository.update(produto);

        log.info("Produto atualizado com sucesso - publicId: {}", publicId);
        return toResponse(produto);
    }

    @Transactional
    public ProdutoResponseDTO inativar(UUID publicId) {
        log.info("Inativando produto - publicId: {}", publicId);

        Produto produto = produtoRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", publicId.toString()));

        produto.desativar();
        produtoRepository.update(produto);

        log.info("Produto inativado com sucesso - publicId: {}", publicId);
        return toResponse(produto);
    }

    @Transactional
    public ProdutoResponseDTO desvincularRfid(UUID produtoPublicId, String codigoRfid) {
        log.info("Desvinculando RFID - produtoPublicId: {}, codigoRfid: {}", produtoPublicId, codigoRfid);

        Produto produto = produtoRepository.findByPublicId(produtoPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoPublicId.toString()));

        tagRfidRepository.desvincular(codigoRfid);

        log.info("RFID desvinculado com sucesso - produto: {}, rfid: {}", produto.getNome(), codigoRfid);
        return toResponse(produto);
    }

    @Transactional
    public ProdutoResponseDTO desvincularTodosRfid(UUID produtoPublicId) {
        log.info("Desvinculando todos os RFID - produtoPublicId: {}", produtoPublicId);

        Produto produto = produtoRepository.findByPublicId(produtoPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoPublicId.toString()));

        tagRfidRepository.desvincularTodos(produto.getId());

        log.info("Todos os RFID desvinculados com sucesso - produto: {}", produto.getNome());
        return toResponse(produto);
    }

    public List<Map<String, Object>> listarTagsRfid(UUID produtoPublicId) {
        log.debug("Listando tags RFID do produto - produtoPublicId: {}", produtoPublicId);

        Produto produto = produtoRepository.findByPublicId(produtoPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoPublicId.toString()));

        var tags = tagRfidRepository.findAtivosByProdutoId(produto.getId());
        return tags.stream().map(tag -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("publicId", tag.getPublicId());
            map.put("codigoRfid", tag.getCodigoRfid());
            map.put("criadoEm", tag.getCriadoEm());
            return map;
        }).toList();
    }

    @Transactional
    public LeituraRfidResponseDTO solicitarLeituraRfid(SolicitarLeituraRfidRequest dto) {
        log.info("Solicitando leitura RFID - produtoPublicId: {}, embarcadoKey: {}",
                dto.produtoPublicId(), dto.embarcadoKey());

        var produto = produtoRepository.findByPublicId(dto.produtoPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", dto.produtoPublicId().toString()));

        int tagsAtivas = tagRfidRepository.findByProdutoId(produto.getId()).size();
        if (tagsAtivas >= produto.getEstoque()) {
            throw new IllegalArgumentException(
                    String.format("Produto ja atingiu o limite de tags RFID vinculadas (%d/%d)", tagsAtivas, produto.getEstoque()));
        }

        if (!webSocketHandler.isEmbarcadoConectado(dto.embarcadoKey())) {
            throw new IllegalArgumentException("Embarcado nao conectado via WebSocket");
        }

        LeituraRfid leitura = LeituraRfid.criar(produto.getLojaId(), dto.embarcadoKey(), TEMPO_EXPIRACAO_LEITURA);
        leitura.setProdutoId(produto.getId());
        Long id = leituraRfidRepository.save(leitura);
        leitura.setId(id);

        CompletableFuture<String> future = new CompletableFuture<>();
        leiturasPendentes.put(leitura.getPublicId(), future);

        webSocketHandler.enviarParaEmbarcado(dto.embarcadoKey(), Map.of(
                "tipo", "SOLICITAR_LEITURA",
                "leituraId", leitura.getPublicId().toString()
        ));

        log.info("Leitura RFID solicitada - leituraId: {}, expiraEm: {}", leitura.getPublicId(), leitura.getExpiraEm());

        return new LeituraRfidResponseDTO(
                leitura.getPublicId(),
                leitura.getStatus().name(),
                leitura.getCodigoRfid(),
                leitura.getExpiraEm()
        );
    }

    public LeituraRfidResponseDTO consultarLeitura(UUID leituraPublicId) {
        log.debug("Consultando leitura RFID - leituraPublicId: {}", leituraPublicId);

        var leitura = leituraRfidRepository.findByPublicId(leituraPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Leitura RFID", leituraPublicId.toString()));

        if (leitura.isExpirado() && leitura.getStatus() == StatusLeituraRfid.PENDENTE) {
            leituraRfidRepository.updateStatus(leitura.getPublicId(), StatusLeituraRfid.EXPIRADO, null);
            leitura.setStatus(StatusLeituraRfid.EXPIRADO);
        }

        return new LeituraRfidResponseDTO(
                leitura.getPublicId(),
                leitura.getStatus().name(),
                leitura.getCodigoRfid(),
                leitura.getExpiraEm()
        );
    }

    @Transactional
    public ProdutoResponseDTO vincularRfid(UUID produtoPublicId, String codigoRfid) {
        log.info("Vinculando RFID ao produto - produtoPublicId: {}, codigoRfid: {}", produtoPublicId, codigoRfid);

        Produto produto = produtoRepository.findByPublicId(produtoPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoPublicId.toString()));

        if (tagRfidRepository.existsByCodigoRfid(codigoRfid)) {
            throw new IllegalArgumentException("Tag RFID ja vinculada a outro produto");
        }

        TagRfid tag = TagRfid.criar(produto.getLojaId(), produto.getId(), codigoRfid);
        tagRfidRepository.save(tag);

        log.info("RFID vinculado com sucesso - produto: {}, rfid: {}", produto.getNome(), codigoRfid);
        return toResponse(produto);
    }

    @EventListener
    public void onLeituraRfid(LeituraRfidEvent event) {
        log.info("Evento de leitura RFID recebido - leituraId: {}, codigoRfid: {}", event.leituraId(), event.codigoRfid());

        var leitura = leituraRfidRepository.findByPublicId(UUID.fromString(event.leituraId()))
                .orElse(null);

        if (leitura == null || leitura.getStatus() != StatusLeituraRfid.PENDENTE) {
            log.warn("Leitura RFID nao encontrada ou ja processada - leituraId: {}", event.leituraId());
            return;
        }

        leituraRfidRepository.updateStatus(leitura.getPublicId(), StatusLeituraRfid.LIDO, event.codigoRfid());

        CompletableFuture<String> future = leiturasPendentes.remove(leitura.getPublicId());
        if (future != null) {
            future.complete(event.codigoRfid());
        }

        log.info("Leitura RFID processada - leituraId: {}, codigoRfid: {}", event.leituraId(), event.codigoRfid());
    }

    @Transactional
    public void cancelarLeitura(UUID leituraPublicId) {
        log.info("Cancelando leitura RFID - leituraPublicId: {}", leituraPublicId);

        var leitura = leituraRfidRepository.findByPublicId(leituraPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Leitura RFID", leituraPublicId.toString()));

        if (leitura.getStatus() != StatusLeituraRfid.PENDENTE) {
            throw new IllegalArgumentException("Leitura nao esta pendente - status atual: " + leitura.getStatus());
        }

        leituraRfidRepository.updateStatus(leitura.getPublicId(), StatusLeituraRfid.CANCELADO, null);

        CompletableFuture<String> future = leiturasPendentes.remove(leitura.getPublicId());
        if (future != null) {
            future.completeExceptionally(new RuntimeException("Leitura cancelada"));
        }

        log.info("Leitura RFID cancelada com sucesso - leituraPublicId: {}", leituraPublicId);
    }

    private ProdutoResponseDTO toResponse(Produto produto) {
        var loja = lojaRepository.findById(produto.getLojaId())
                .orElse(null);
        UUID lojaPublicId = loja != null ? loja.getPublicId() : null;

        int tagsVinculadas = tagRfidRepository.findByProdutoId(produto.getId()).size();

        return new ProdutoResponseDTO(
                produto.getPublicId(),
                lojaPublicId,
                produto.getCodigoBarras(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getEstoque(),
                tagsVinculadas,
                produto.getAtivo(),
                produto.getDataValidade(),
                produto.getCriadoEm()
        );
    }
}
