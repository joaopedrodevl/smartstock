package br.com.smartstock.smartstock.infra.api.controllers.v1.admin;

import br.com.smartstock.smartstock.application.services.LojaService;
import br.com.smartstock.smartstock.core.ports.IAdminRepository;
import br.com.smartstock.smartstock.infra.api.dtos.LojaRequestDTO;
import br.com.smartstock.smartstock.infra.api.dtos.LojaResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.PagedResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/lojas")
@Tag(name = "Lojas", description = "Gerenciamento de mini mercados de condomínio")
public class LojaController {

    private static final Logger log = LoggerFactory.getLogger(LojaController.class);

    private final LojaService lojaService;
    private final IAdminRepository adminRepository;

    public LojaController(LojaService lojaService, IAdminRepository adminRepository) {
        this.lojaService = lojaService;
        this.adminRepository = adminRepository;
    }

    @PostMapping
    @Operation(summary = "Criar loja", description = "Cria uma nova loja com endereço vinculado ao administrador")
    public ResponseEntity<LojaResponseDTO> criar(
            @RequestBody @Valid LojaRequestDTO dto,
            Authentication authentication,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {
        Long administradorId = extrairAdministradorId(authentication);
        log.info("Requisição para criar loja - adminId: {}, cnpj: {}", administradorId, dto.cnpj());
        var loja = lojaService.criar(administradorId, dto);
        return ResponseEntity.ok(loja);
    }

    @GetMapping
    @Operation(summary = "Listar lojas", description = "Lista as lojas do administrador autenticado com paginação")
    public ResponseEntity<PagedResponseDTO<LojaResponseDTO>> listar(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Long administradorId = extrairAdministradorId(authentication);
        log.info("Requisição para listar lojas - adminId: {}, page: {}, size: {}", administradorId, page, size);
        var lojas = lojaService.listarPorAdministrador(administradorId, page, size);
        return ResponseEntity.ok(lojas);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Buscar loja por public ID", description = "Retorna os dados de uma loja específica")
    public ResponseEntity<LojaResponseDTO> buscarPorPublicId(
            @PathVariable UUID publicId,
            Authentication authentication) {
        Long administradorId = extrairAdministradorId(authentication);
        log.info("Requisição para buscar loja - publicId: {}, adminId: {}", publicId, administradorId);
        var loja = lojaService.buscarPorPublicId(publicId, administradorId);
        return ResponseEntity.ok(loja);
    }

    @PutMapping("/{publicId}")
    @Operation(summary = "Atualizar loja", description = "Atualiza os dados da loja e seu endereço")
    public ResponseEntity<LojaResponseDTO> atualizar(
            @PathVariable UUID publicId,
            @RequestBody @Valid LojaRequestDTO dto,
            Authentication authentication,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {
        Long administradorId = extrairAdministradorId(authentication);
        log.info("Requisição para atualizar loja - publicId: {}, adminId: {}", publicId, administradorId);
        var loja = lojaService.atualizarPorPublicId(publicId, administradorId, dto);
        return ResponseEntity.ok(loja);
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Desativar loja", description = "Desativa uma loja (soft delete)")
    public ResponseEntity<Void> desativar(
            @PathVariable UUID publicId,
            Authentication authentication,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {
        Long administradorId = extrairAdministradorId(authentication);
        log.info("Requisição para desativar loja - publicId: {}, adminId: {}", publicId, administradorId);
        lojaService.desativarPorPublicId(publicId, administradorId);
        return ResponseEntity.noContent().build();
    }

    private Long extrairAdministradorId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getCredentials();
        String email = jwt.getSubject();
        var admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));
        return admin.getId();
    }
}
