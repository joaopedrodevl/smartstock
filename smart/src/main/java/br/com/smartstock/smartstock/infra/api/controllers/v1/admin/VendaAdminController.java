package br.com.smartstock.smartstock.infra.api.controllers.v1.admin;

import br.com.smartstock.smartstock.application.services.VendaService;
import br.com.smartstock.smartstock.infra.api.dtos.venda.RegistrarVendaRequest;
import br.com.smartstock.smartstock.infra.api.dtos.venda.VendaResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/vendas")
@Tag(name = "Vendas (Admin)", description = "Gerenciamento de vendas pelos administradores")
public class VendaAdminController {

    private static final Logger log = LoggerFactory.getLogger(VendaAdminController.class);

    private final VendaService vendaService;

    public VendaAdminController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @PostMapping
    @Operation(summary = "Registrar venda", description = "Registra uma venda finalizada. Os RFID sao desvinculados dos produtos e salvos no historico.")
    public ResponseEntity<VendaResponseDTO> registrar(
            @RequestBody @Valid RegistrarVendaRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para registrar venda - lojaPublicId: {}, itens: {}", dto.lojaPublicId(), dto.codigosRfid().size());
        var venda = vendaService.registrarVenda(dto);
        return ResponseEntity.ok(venda);
    }

    @GetMapping
    @Operation(summary = "Listar vendas da loja", description = "Lista o historico de vendas de uma loja")
    public ResponseEntity<List<VendaResponseDTO>> listar(
            @RequestParam UUID lojaPublicId) {

        log.info("Requisicao para listar vendas - lojaPublicId: {}", lojaPublicId);
        var vendas = vendaService.listarPorLoja(lojaPublicId);
        return ResponseEntity.ok(vendas);
    }
}
