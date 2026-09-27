package br.com.smartstock.smartstock.infra.api.controllers.v1;

import br.com.smartstock.smartstock.application.services.VendaAdminService;
import br.com.smartstock.smartstock.infra.api.dtos.produto.LeituraRfidResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.produto.ProdutoResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.produto.CancelarLeituraRequest;
import br.com.smartstock.smartstock.infra.api.dtos.venda.*;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.EmbarcadoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/vendas")
@Tag(name = "Vendas", description = "Endpoints para o terminal de venda")
public class VendaController {

    private static final Logger log = LoggerFactory.getLogger(VendaController.class);

    private final VendaAdminService vendaAdminService;

    public VendaController(VendaAdminService vendaAdminService) {
        this.vendaAdminService = vendaAdminService;
    }

    @PostMapping("/rfid/solicitar-leitura")
    @Operation(summary = "Solicitar leitura RFID para venda", description = "Envia comando para o ESP32 de VENDA ler uma tag RFID. Timeout de 30 segundos.")
    public ResponseEntity<LeituraRfidResponseDTO> solicitarLeituraRfid(
            @RequestBody @Valid SolicitarLeituraVendaRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para solicitar leitura RFID (venda) - embarcadoKey: {}", dto.embarcadoKey());
        var leitura = vendaAdminService.solicitarLeituraRfid(dto);
        return ResponseEntity.ok(leitura);
    }

    @GetMapping("/rfid/leitura/{leituraPublicId}")
    @Operation(summary = "Consultar leitura RFID", description = "Consulta o status de uma leitura RFID pendente")
    public ResponseEntity<LeituraRfidResponseDTO> consultarLeitura(
            @PathVariable UUID leituraPublicId) {

        log.debug("Requisicao para consultar leitura RFID (venda) - leituraPublicId: {}", leituraPublicId);
        var leitura = vendaAdminService.consultarLeitura(leituraPublicId);
        return ResponseEntity.ok(leitura);
    }

    @PostMapping("/rfid/cancelar")
    @Operation(summary = "Cancelar leitura RFID", description = "Cancela uma leitura RFID pendente na venda")
    public ResponseEntity<Void> cancelarLeitura(
            @RequestBody @Valid CancelarLeituraRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para cancelar leitura RFID (venda) - leituraPublicId: {}", dto.leituraPublicId());
        vendaAdminService.cancelarLeitura(dto.leituraPublicId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cliente/consultar")
    @Operation(summary = "Consultar cliente por CPF", description = "Verifica se o CPF existe no sistema. Se existir, retorna os dados do cliente.")
    public ResponseEntity<ConsultarClienteResponse> consultarCliente(
            @RequestBody @Valid ConsultarClienteRequest dto) {
        log.info("Requisicao para consultar cliente - cpf: {}", dto.cpf());
        var response = vendaAdminService.consultarCliente(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/produto/buscar")
    @Operation(summary = "Buscar produto por RFID", description = "Busca o produto associado a uma tag RFID para venda")
    public ResponseEntity<ProdutoResponseDTO> buscarProdutoPorRfid(
            @RequestParam String codigoRfid) {
        log.info("Requisicao para buscar produto por RFID - codigoRfid: {}", codigoRfid);
        var produto = vendaAdminService.buscarProdutoPorRfid(codigoRfid);
        return ResponseEntity.ok(produto);
    }

    @PostMapping("/criar")
    @Operation(summary = "Criar venda", description = "Cria uma venda com cliente e itens. Se o CPF for novo, o telefone e obrigatoria.")
    public ResponseEntity<VendaResponseDTO> criarVenda(
            @RequestBody @Valid CriarVendaRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {
        log.info("Requisicao para criar venda - lojaPublicId: {}, cpf: {}, itens: {}",
                dto.lojaPublicId(), dto.cpfCliente(), dto.codigosRfid().size());
        var venda = vendaAdminService.criarVenda(dto);
        return ResponseEntity.ok(venda);
    }

    @PostMapping("/pagamento")
    @Operation(summary = "Processar pagamento", description = "Processa o pagamento de uma venda (simulado). Aprova sempre.")
    public ResponseEntity<PagamentoResponse> processarPagamento(
            @RequestBody @Valid PagamentoRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {
        log.info("Requisicao para processar pagamento - vendaPublicId: {}, formaPagamento: {}",
                dto.vendaPublicId(), dto.formaPagamento());
        var pagamento = vendaAdminService.processarPagamento(dto);
        return ResponseEntity.ok(pagamento);
    }

    @GetMapping("/historico")
    @Operation(summary = "Listar historico de vendas", description = "Lista o historico de vendas de uma loja")
    public ResponseEntity<List<VendaResponseDTO>> listar(
            @RequestParam UUID lojaPublicId) {
        log.info("Requisicao para listar vendas - lojaPublicId: {}", lojaPublicId);
        var vendas = vendaAdminService.listarPorLoja(lojaPublicId);
        return ResponseEntity.ok(vendas);
    }

    @GetMapping("/minha-loja")
    @Operation(summary = "Minha loja", description = "Retorna a loja vinculada ao vendedor logado (ROLE_VENDA_ADMIN)")
    public ResponseEntity<MinhaLojaResponse> minhaLoja() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Requisicao para buscar loja do vendedor - email: {}", email);
        return ResponseEntity.ok(vendaAdminService.minhaLoja(email));
    }

    @GetMapping("/embarcados")
    @Operation(summary = "Meus embarcados", description = "Lista os embarcados da loja vinculada ao vendedor logado")
    public ResponseEntity<List<EmbarcadoResponseDTO>> meusEmbarcados() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Requisicao para listar embarcados do vendedor - email: {}", email);
        return ResponseEntity.ok(vendaAdminService.meusEmbarcados(email));
    }
}
