package br.com.smartstock.smartstock.infra.api.controllers.v1.admin;

import br.com.smartstock.smartstock.application.services.AdminService;
import br.com.smartstock.smartstock.infra.api.dtos.AdminRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.AdminResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.VendaAdminRequestDTO;
import br.com.smartstock.smartstock.application.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@Tag(name = "Administradores", description = "Gerenciamento de administradores")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    @Operation(summary = "Criar administrador", description = "Cria um novo administrador no sistema")
    public ResponseEntity<AdminResponseDTO> create(
            @RequestBody @Valid AdminRequestDTO admin,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {
        log.info("Requisição para criar administrador - email: {}", admin.email());
        var newAdmin = adminService.criar(admin);
        return ResponseEntity.ok(AdminResponseDTO.from(newAdmin));
    }

    @PostMapping("/venda-admins")
    @Operation(summary = "Criar vendedor de loja", description = "Cria um administrador ROLE_VENDA_ADMIN vinculado a uma loja (um por loja)")
    public ResponseEntity<AdminResponseDTO> criarVendaAdmin(
            @RequestBody @Valid VendaAdminRequestDTO dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {
        log.info("Requisição para criar vendedor - email: {}, lojaPublicId: {}", dto.email(), dto.lojaPublicId());
        var newAdmin = adminService.criarVendaAdmin(dto);
        return ResponseEntity.ok(AdminResponseDTO.from(newAdmin));
    }

    @GetMapping("/venda-admins")
    @Operation(summary = "Buscar vendedor da loja", description = "Retorna o vendedor (ROLE_VENDA_ADMIN) vinculado à loja, se existir")
    public ResponseEntity<AdminResponseDTO> buscarVendaAdmin(
            @RequestParam UUID lojaPublicId) {
        log.info("Requisição para buscar vendedor da loja - lojaPublicId: {}", lojaPublicId);
        var admin = adminService.buscarVendaAdminPorLoja(lojaPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor da loja", lojaPublicId.toString()));
        return ResponseEntity.ok(AdminResponseDTO.from(admin));
    }
}
