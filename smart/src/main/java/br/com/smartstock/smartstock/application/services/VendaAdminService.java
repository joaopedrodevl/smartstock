package br.com.smartstock.smartstock.application.services;

import br.com.smartstock.smartstock.application.exceptions.ResourceNotFoundException;
import br.com.smartstock.smartstock.core.entities.Admin;
import br.com.smartstock.smartstock.core.entities.Cliente;
import br.com.smartstock.smartstock.core.entities.HistoricoVenda;
import br.com.smartstock.smartstock.core.entities.Embarcado;
import br.com.smartstock.smartstock.core.entities.LeituraRfid;
import br.com.smartstock.smartstock.core.entities.Produto;
import br.com.smartstock.smartstock.core.entities.TagRfid;
import br.com.smartstock.smartstock.core.entities.Venda;
import br.com.smartstock.smartstock.core.enums.FormaPagamento;
import br.com.smartstock.smartstock.core.enums.StatusLeituraRfid;
import br.com.smartstock.smartstock.core.ports.*;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.EmbarcadoResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.produto.LeituraRfidResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.produto.ProdutoResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.venda.*;
import br.com.smartstock.smartstock.infra.websocket.EmbarcadoWebSocketHandler;
import br.com.smartstock.smartstock.infra.websocket.LeituraRfidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VendaAdminService {

    private static final Logger log = LoggerFactory.getLogger(VendaAdminService.class);
    private static final int TEMPO_EXPIRACAO_LEITURA = 30;

    private final ILojaRepository lojaRepository;
    private final IClienteRepository clienteRepository;
    private final ITagRfidRepository tagRfidRepository;
    private final IProdutoRepository produtoRepository;
    private final IVendaRepository vendaRepository;
    private final IHistoricoVendaRepository historicoVendaRepository;
    private final ILeituraRfidRepository leituraRfidRepository;
    private final IEmbarcadoRepository embarcadoRepository;
    private final IAdminRepository adminRepository;
    private final EmbarcadoService embarcadoService;
    private final EmbarcadoWebSocketHandler webSocketHandler;

    private final ConcurrentHashMap<UUID, CompletableFuture<String>> leiturasPendentes = new ConcurrentHashMap<>();

    public VendaAdminService(ILojaRepository lojaRepository, IClienteRepository clienteRepository,
                              ITagRfidRepository tagRfidRepository, IProdutoRepository produtoRepository,
                              IVendaRepository vendaRepository, IHistoricoVendaRepository historicoVendaRepository,
                              ILeituraRfidRepository leituraRfidRepository, IEmbarcadoRepository embarcadoRepository,
                              IAdminRepository adminRepository, EmbarcadoService embarcadoService,
                              EmbarcadoWebSocketHandler webSocketHandler) {
        this.lojaRepository = lojaRepository;
        this.clienteRepository = clienteRepository;
        this.tagRfidRepository = tagRfidRepository;
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.historicoVendaRepository = historicoVendaRepository;
        this.leituraRfidRepository = leituraRfidRepository;
        this.embarcadoRepository = embarcadoRepository;
        this.adminRepository = adminRepository;
        this.embarcadoService = embarcadoService;
        this.webSocketHandler = webSocketHandler;
    }

    @Transactional
    public LeituraRfidResponseDTO solicitarLeituraRfid(SolicitarLeituraVendaRequest dto) {
        log.info("Solicitando leitura RFID para venda - embarcadoKey: {}", dto.embarcadoKey());

        if (!webSocketHandler.isEmbarcadoConectado(dto.embarcadoKey())) {
            throw new IllegalArgumentException("Embarcado nao conectado via WebSocket");
        }

        Embarcado embarcado = embarcadoRepository.findByChavePublica(dto.embarcadoKey())
                .orElseThrow(() -> new IllegalArgumentException("Embarcado nao encontrado"));

        LeituraRfid leitura = LeituraRfid.criar(embarcado.getLojaId(), dto.embarcadoKey(), TEMPO_EXPIRACAO_LEITURA);
        Long id = leituraRfidRepository.save(leitura);
        leitura.setId(id);

        CompletableFuture<String> future = new CompletableFuture<>();
        leiturasPendentes.put(leitura.getPublicId(), future);

        webSocketHandler.enviarParaEmbarcado(dto.embarcadoKey(), Map.of(
                "tipo", "SOLICITAR_LEITURA",
                "leituraId", leitura.getPublicId().toString()
        ));

        log.info("Leitura RFID solicitada para venda - leituraId: {}, expiraEm: {}", leitura.getPublicId(), leitura.getExpiraEm());

        return new LeituraRfidResponseDTO(
                leitura.getPublicId(),
                leitura.getStatus().name(),
                leitura.getCodigoRfid(),
                leitura.getExpiraEm()
        );
    }

    public LeituraRfidResponseDTO consultarLeitura(UUID leituraPublicId) {
        log.debug("Consultando leitura RFID para venda - leituraPublicId: {}", leituraPublicId);

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

    @EventListener
    public void onLeituraRfid(LeituraRfidEvent event) {
        log.info("Evento de leitura RFID recebido (venda) - leituraId: {}, codigoRfid: {}", event.leituraId(), event.codigoRfid());

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

        log.info("Leitura RFID processada (venda) - leituraId: {}, codigoRfid: {}", event.leituraId(), event.codigoRfid());
    }

    @Transactional
    public void cancelarLeitura(UUID leituraPublicId) {
        log.info("Cancelando leitura RFID (venda) - leituraPublicId: {}", leituraPublicId);

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

        log.info("Leitura RFID cancelada com sucesso (venda) - leituraPublicId: {}", leituraPublicId);
    }

    @Transactional
    public ConsultarClienteResponse consultarCliente(ConsultarClienteRequest dto) {
        log.info("Consultando cliente por CPF - cpf: {}", dto.cpf());
        return clienteRepository.findByCpf(dto.cpf())
                .map(c -> ConsultarClienteResponse.encontrado(c.getCpf(), c.getTelefone(), c.getNome(), c.getCriadoEm()))
                .orElse(ConsultarClienteResponse.naoEncontrado(dto.cpf()));
    }

    @Transactional
    public ProdutoResponseDTO buscarProdutoPorRfid(String codigoRfid) {
        log.info("Buscando produto por RFID - codigoRfid: {}", codigoRfid);
        TagRfid tag = tagRfidRepository.findByCodigoRfid(codigoRfid)
                .orElseThrow(() -> new ResourceNotFoundException("Tag RFID", codigoRfid));
        if (!tag.getAtivo()) {
            throw new IllegalArgumentException("Tag RFID ja foi vendida: " + codigoRfid);
        }
        Produto produto = produtoRepository.findById(tag.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", tag.getProdutoId().toString()));
        if (!produto.getAtivo()) {
            throw new IllegalArgumentException("Produto inativo: " + produto.getNome());
        }

        var loja = lojaRepository.findById(produto.getLojaId()).orElse(null);
        UUID lojaPublicId = loja != null ? loja.getPublicId() : null;

        int tagsVinculadas = tagRfidRepository.findByProdutoId(produto.getId()).size();

        return new ProdutoResponseDTO(produto.getPublicId(), lojaPublicId, produto.getCodigoBarras(),
                produto.getNome(), produto.getDescricao(), produto.getPreco(), produto.getEstoque(),
                tagsVinculadas, produto.getAtivo(), produto.getDataValidade(), produto.getCriadoEm());
    }

    @Transactional
    public VendaResponseDTO criarVenda(CriarVendaRequest dto) {
        log.info("Criando venda - lojaPublicId: {}, cpf: {}, itens: {}",
                dto.lojaPublicId(), dto.cpfCliente(), dto.codigosRfid().size());

        var loja = lojaRepository.findByPublicId(dto.lojaPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Loja", dto.lojaPublicId().toString()));

        Cliente cliente = clienteRepository.findByCpf(dto.cpfCliente())
                .orElseGet(() -> {
                    if (dto.telefoneCliente() == null || dto.telefoneCliente().isBlank()) {
                        throw new IllegalArgumentException("Telefone e obrigatorio para clientes novos");
                    }
                    Cliente novo = Cliente.criar(dto.cpfCliente(), dto.telefoneCliente(), null);
                    Long id = clienteRepository.save(novo);
                    novo.setId(id);
                    return novo;
                });

        Venda venda = Venda.criar(loja.getId(), cliente.getId());
        Long vendaId = vendaRepository.save(venda);
        venda.setId(vendaId);

        List<HistoricoVenda> historicos = new ArrayList<>();
        List<VendaResponseDTO.ItemVendaResponseDTO> itensResponse = new ArrayList<>();

        for (String codigoRfid : dto.codigosRfid()) {
            TagRfid tag = tagRfidRepository.findByCodigoRfid(codigoRfid)
                    .orElseThrow(() -> new IllegalArgumentException("Tag RFID nao encontrada: " + codigoRfid));
            if (!tag.getAtivo()) {
                throw new IllegalArgumentException("Tag RFID ja vendida: " + codigoRfid);
            }
            Produto produto = produtoRepository.findById(tag.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado para RFID: " + codigoRfid));
            if (!produto.getAtivo()) {
                throw new IllegalArgumentException("Produto inativo: " + produto.getNome());
            }

            historicos.add(HistoricoVenda.criar(loja.getId(), produto.getId(), codigoRfid, produto.getPreco()));
            itensResponse.add(new VendaResponseDTO.ItemVendaResponseDTO(codigoRfid, produto.getNome(), produto.getPreco()));
            venda.adicionarValor(produto.getPreco());
            tagRfidRepository.desvincular(codigoRfid);
            produto.decrementarEstoque(1);
            produtoRepository.update(produto);
        }

        vendaRepository.update(venda);
        historicoVendaRepository.saveAll(historicos);

        log.info("Venda criada com sucesso - vendaId: {}, valorTotal: {}", vendaId, venda.getValorTotal());

        return new VendaResponseDTO(venda.getPublicId(), loja.getPublicId(), venda.getValorTotal(),
                venda.getStatus().name(), venda.getFormaPagamento().name(), venda.getCriadoEm(), itensResponse);
    }

    @Transactional
    public PagamentoResponse processarPagamento(PagamentoRequest dto) {
        log.info("Processando pagamento - vendaPublicId: {}, formaPagamento: {}", dto.vendaPublicId(), dto.formaPagamento());

        Venda venda = vendaRepository.findByPublicId(dto.vendaPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Venda", dto.vendaPublicId().toString()));

        if (venda.getStatus() != br.com.smartstock.smartstock.core.enums.StatusVenda.aberta) {
            throw new IllegalArgumentException("Venda nao esta aberta");
        }

        FormaPagamento formaPagamento;
        try {
            formaPagamento = FormaPagamento.valueOf(dto.formaPagamento());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Forma de pagamento invalida: " + dto.formaPagamento());
        }

        venda.finalizar(formaPagamento);
        vendaRepository.update(venda);

        log.info("Pagamento processado com sucesso - vendaId: {}", venda.getId());

        return new PagamentoResponse(venda.getPublicId(), "aprovado", formaPagamento.name(),
                venda.getValorTotal(), LocalDateTime.now());
    }

    public List<VendaResponseDTO> listarPorLoja(UUID lojaPublicId) {
        log.info("Listando vendas da loja - lojaPublicId: {}", lojaPublicId);
        var loja = lojaRepository.findByPublicId(lojaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Loja", lojaPublicId.toString()));

        List<Venda> vendas = vendaRepository.findByLojaId(loja.getId());

        return vendas.stream()
                .map(v -> new VendaResponseDTO(
                        v.getPublicId(),
                        loja.getPublicId(),
                        v.getValorTotal(),
                        v.getStatus().name(),
                        v.getFormaPagamento().name(),
                        v.getCriadoEm(),
                        List.of()))
                .toList();
    }

    public MinhaLojaResponse minhaLoja(String email) {
        log.info("Buscando loja do vendedor - email: {}", email);
        var loja = lojaRepository.findById(lojaIdDoVendedor(email))
                .orElseThrow(() -> new ResourceNotFoundException("Loja", "do vendedor " + email));
        return new MinhaLojaResponse(loja.getPublicId(), loja.getNomeFantasia().valor());
    }

    public List<EmbarcadoResponseDTO> meusEmbarcados(String email) {
        log.info("Listando embarcados do vendedor - email: {}", email);
        var loja = lojaRepository.findById(lojaIdDoVendedor(email))
                .orElseThrow(() -> new ResourceNotFoundException("Loja", "do vendedor " + email));
        return embarcadoService.listarPorLoja(loja.getPublicId());
    }

    private Long lojaIdDoVendedor(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador", email));
        if (admin.getLojaId() == null) {
            throw new IllegalArgumentException("Este administrador não está vinculado a uma loja");
        }
        return admin.getLojaId();
    }
}
