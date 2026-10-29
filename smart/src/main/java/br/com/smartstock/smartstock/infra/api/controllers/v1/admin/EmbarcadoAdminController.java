package br.com.smartstock.smartstock.infra.api.controllers.v1.admin;

import br.com.smartstock.smartstock.application.services.EmbarcadoService;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.CreateEmbarcadoRequest;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.EmbarcadoResponseDTO;
import br.com.smartstock.smartstock.infra.websocket.EmbarcadoWebSocketHandler;
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
@RequestMapping("/v1/admin/embarcados")
@Tag(name = "Embarcados (Admin)", description = "Gerenciamento de embarcados ESP32 pelos administradores")
public class EmbarcadoAdminController {

    private static final Logger log = LoggerFactory.getLogger(EmbarcadoAdminController.class);

    private final EmbarcadoService embarcadoService;
    private final EmbarcadoWebSocketHandler webSocketHandler;

    public EmbarcadoAdminController(EmbarcadoService embarcadoService,
                                     EmbarcadoWebSocketHandler webSocketHandler) {
        this.embarcadoService = embarcadoService;
        this.webSocketHandler = webSocketHandler;
    }

    @PostMapping
    @Operation(summary = "Cadastrar embarcado", description = "Cadastra um novo embarcado ESP32 vinculado a uma loja. O admin deve gerar as chaves (chavePublica + hmacSecret) e gravar no eFuse do ESP32")
    public ResponseEntity<EmbarcadoResponseDTO> cadastrar(
            @RequestBody @Valid CreateEmbarcadoRequest dto,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para cadastrar embarcado - lojaPublicId: {}, tipo: {}",
                dto.lojaPublicId(), dto.tipo());

        var embarcado = embarcadoService.cadastrar(
                dto.lojaPublicId(),
                dto.tipo(),
                dto.descricao()
        );

        return ResponseEntity.ok(embarcado);
    }

    @GetMapping
    @Operation(summary = "Listar embarcados da loja", description = "Lista todos os embarcados de uma loja especifica")
    public ResponseEntity<List<EmbarcadoResponseDTO>> listar(
            @RequestParam UUID lojaPublicId) {

        log.info("Requisicao para listar embarcados - lojaPublicId: {}", lojaPublicId);
        var embarcados = embarcadoService.listarPorLoja(lojaPublicId);
        return ResponseEntity.ok(embarcados);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Buscar embarcado", description = "Retorna os dados de um embarcado especifico")
    public ResponseEntity<EmbarcadoResponseDTO> buscarPorPublicId(
            @PathVariable UUID publicId) {

        log.info("Requisicao para buscar embarcado - publicId: {}", publicId);
        var embarcado = embarcadoService.buscarPorPublicId(publicId);
        return ResponseEntity.ok(embarcado);
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Remover embarcado", description = "Remove um embarcado do sistema")
    public ResponseEntity<Void> remover(
            @PathVariable UUID publicId,
            @Parameter(description = "Token CSRF obtido do cookie XSRF-TOKEN", required = true)
            @RequestHeader("X-XSRF-TOKEN") String xsrfToken) {

        log.info("Requisicao para remover embarcado - publicId: {}", publicId);
        embarcadoService.remover(publicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    @Operation(summary = "Status de conexao dos embarcados", description = "Retorna o status online/offline de todos os embarcados de uma loja")
    public ResponseEntity<Map<String, Boolean>> statusConexao(
            @RequestParam UUID lojaPublicId) {

        log.info("Requisicao para verificar status dos embarcados - lojaPublicId: {}", lojaPublicId);
        var embarcados = embarcadoService.listarPorLoja(lojaPublicId);
        var status = embarcados.stream()
                .collect(java.util.stream.Collectors.toMap(
                        EmbarcadoResponseDTO::chavePublica,
                        e -> webSocketHandler.isEmbarcadoConectado(e.chavePublica())
                ));
        return ResponseEntity.ok(status);
    }
}
