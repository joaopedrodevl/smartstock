package br.com.smartstock.smartstock.infra.api.controllers.v1.admin;

import br.com.smartstock.smartstock.application.services.ProdutoService;
import br.com.smartstock.smartstock.infra.api.dtos.produto.AtualizarProdutoRequest;
import br.com.smartstock.smartstock.infra.api.dtos.produto.CriarProdutoRequest;
import br.com.smartstock.smartstock.infra.api.dtos.produto.LeituraRfidResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.produto.ProdutoResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.produto.SolicitarLeituraRfidRequest;
import br.com.smartstock.smartstock.infra.api.dtos.produto.CancelarLeituraRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/produtos")
@Tag(name = "Produtos (Admin)", description = "Gerenciamento de produtos pelos administradores")
public class ProdutoAdminController {

    private static final Logger log = LoggerFactory.getLogger(ProdutoAdminController.class);

    private final ProdutoService produtoService;

    public ProdutoAdminController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    @Operation(summary = "Criar produto", description = "Cria um novo produto vinculado a uma loja")
    public ResponseEntity<ProdutoResponseDTO> criar(
            @RequestBody @Valid CriarProdutoRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para criar produto - lojaPublicId: {}, nome: {}", dto.lojaPublicId(), dto.nome());
        var produto = produtoService.criar(dto);
        return ResponseEntity.ok(produto);
    }

    @GetMapping
    @Operation(summary = "Listar produtos da loja", description = "Lista todos os produtos de uma loja")
    public ResponseEntity<List<ProdutoResponseDTO>> listar(
            @RequestParam UUID lojaPublicId) {

        log.info("Requisicao para listar produtos - lojaPublicId: {}", lojaPublicId);
        var produtos = produtoService.listarPorLoja(lojaPublicId);
        return ResponseEntity.ok(produtos);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Buscar produto", description = "Retorna os dados de um produto especifico")
    public ResponseEntity<ProdutoResponseDTO> buscarPorPublicId(
            @PathVariable UUID publicId) {

        log.info("Requisicao para buscar produto - publicId: {}", publicId);
        var produto = produtoService.buscarPorPublicId(publicId);
        return ResponseEntity.ok(produto);
    }

    @PutMapping("/{publicId}")
    @Operation(summary = "Atualizar produto", description = "Atualiza os dados de um produto existente")
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @PathVariable UUID publicId,
            @RequestBody @Valid AtualizarProdutoRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para atualizar produto - publicId: {}, nome: {}", publicId, dto.nome());
        var produto = produtoService.atualizar(publicId, dto);
        return ResponseEntity.ok(produto);
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Inativar produto", description = "Inativa um produto existente")
    public ResponseEntity<ProdutoResponseDTO> inativar(
            @PathVariable UUID publicId,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para inativar produto - publicId: {}", publicId);
        var produto = produtoService.inativar(publicId);
        return ResponseEntity.ok(produto);
    }

    @PostMapping("/rfid/solicitar-leitura")
    @Operation(summary = "Solicitar leitura RFID", description = "Envia comando para o ESP32 ler uma tag RFID. Timeout de 30 segundos.")
    public ResponseEntity<LeituraRfidResponseDTO> solicitarLeituraRfid(
            @RequestBody @Valid SolicitarLeituraRfidRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para solicitar leitura RFID - produtoPublicId: {}, embarcadoKey: {}",
                dto.produtoPublicId(), dto.embarcadoKey());
        var leitura = produtoService.solicitarLeituraRfid(dto);
        return ResponseEntity.ok(leitura);
    }

    @GetMapping("/rfid/leitura/{leituraPublicId}")
    @Operation(summary = "Consultar leitura RFID", description = "Consulta o status de uma leitura RFID pendente")
    public ResponseEntity<LeituraRfidResponseDTO> consultarLeitura(
            @PathVariable UUID leituraPublicId) {

        log.debug("Requisicao para consultar leitura RFID - leituraPublicId: {}", leituraPublicId);
        var leitura = produtoService.consultarLeitura(leituraPublicId);
        return ResponseEntity.ok(leitura);
    }

    @PostMapping("/rfid/vincular")
    @Operation(summary = "Vincular RFID ao produto", description = "Vincula uma tag RFID a um produto existente")
    public ResponseEntity<ProdutoResponseDTO> vincularRfid(
            @RequestParam UUID produtoPublicId,
            @RequestParam String codigoRfid,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para vincular RFID - produtoPublicId: {}, codigoRfid: {}", produtoPublicId, codigoRfid);
        var produto = produtoService.vincularRfid(produtoPublicId, codigoRfid);
        return ResponseEntity.ok(produto);
    }

    @DeleteMapping("/rfid/desvincular")
    @Operation(summary = "Desvincular RFID", description = "Desvincula uma tag RFID de um produto")
    public ResponseEntity<ProdutoResponseDTO> desvincularRfid(
            @RequestParam UUID produtoPublicId,
            @RequestParam String codigoRfid,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para desvincular RFID - produtoPublicId: {}, codigoRfid: {}", produtoPublicId, codigoRfid);
        var produto = produtoService.desvincularRfid(produtoPublicId, codigoRfid);
        return ResponseEntity.ok(produto);
    }

    @DeleteMapping("/rfid/desvincular-todos")
    @Operation(summary = "Desvincular todos os RFID", description = "Desvincula todas as tags RFID de um produto")
    public ResponseEntity<ProdutoResponseDTO> desvincularTodosRfid(
            @RequestParam UUID produtoPublicId,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para desvincular todos os RFID - produtoPublicId: {}", produtoPublicId);
        var produto = produtoService.desvincularTodosRfid(produtoPublicId);
        return ResponseEntity.ok(produto);
    }

    @GetMapping("/rfid/tags/{produtoPublicId}")
    @Operation(summary = "Listar tags RFID", description = "Lista todas as tags RFID vinculadas a um produto")
    public ResponseEntity<List<Map<String, Object>>> listarTagsRfid(
            @PathVariable UUID produtoPublicId) {

        log.debug("Requisicao para listar tags RFID - produtoPublicId: {}", produtoPublicId);
        var tags = produtoService.listarTagsRfid(produtoPublicId);
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/rfid/cancelar")
    @Operation(summary = "Cancelar leitura RFID", description = "Cancela uma leitura RFID pendente")
    public ResponseEntity<Void> cancelarLeitura(
            @RequestBody @Valid CancelarLeituraRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para cancelar leitura RFID - leituraPublicId: {}", dto.leituraPublicId());
        produtoService.cancelarLeitura(dto.leituraPublicId());
        return ResponseEntity.ok().build();
    }
}
