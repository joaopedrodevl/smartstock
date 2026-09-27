package br.com.smartstock.smartstock.infra.api.controllers.v1;

import br.com.smartstock.smartstock.application.services.EmbarcadoAuthService;
import br.com.smartstock.smartstock.application.services.EmbarcadoService;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.AuthEmbarcadoRequest;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.AuthEmbarcadoResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.CadastrarProdutoRequest;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.EmbarcadoHeartbeatResponseDTO;
import br.com.smartstock.smartstock.infra.api.dtos.embarcado.RegistrarVendaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/embarcado")
@Tag(name = "Embarcados", description = "Endpoints para dispositivos ESP32")
public class EmbarcadoController {

    private static final Logger log = LoggerFactory.getLogger(EmbarcadoController.class);

    private final EmbarcadoAuthService embarcadoAuthService;
    private final EmbarcadoService embarcadoService;

    public EmbarcadoController(EmbarcadoAuthService embarcadoAuthService, EmbarcadoService embarcadoService) {
        this.embarcadoAuthService = embarcadoAuthService;
        this.embarcadoService = embarcadoService;
    }

    @PostMapping("/auth")
    @Operation(summary = "Autenticar embarcado", description = "Autentica um dispositivo ESP32 e retorna um token de sessao")
    public ResponseEntity<AuthEmbarcadoResponseDTO> autenticar(
            @RequestBody @Valid AuthEmbarcadoRequest dto) {

        log.info("Requisicao de autenticacao - chavePublica: {}", dto.chavePublica());
        var response = embarcadoAuthService.autenticar(dto.chavePublica());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Heartbeat", description = "Verifica se o dispositivo esta ativo e conectado")
    public ResponseEntity<EmbarcadoHeartbeatResponseDTO> heartbeat(
            @Parameter(description = "Token de sessao obtido no /auth", required = true)
            @RequestHeader("X-Embarcado-Token") String token,
            Authentication authentication) {

        String chavePublica = (String) authentication.getPrincipal();
        log.debug("Heartbeat recebido - chavePublica: {}", chavePublica);
        var response = embarcadoService.heartbeat(chavePublica);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/venda")
    @Operation(summary = "Registrar venda", description = "Registra uma venda realizada pelo dispositivo de venda")
    public ResponseEntity<Void> registrarVenda(
            @RequestBody @Valid RegistrarVendaRequest dto,
            @Parameter(description = "Token de sessao obtido no /auth", required = true)
            @RequestHeader("X-Embarcado-Token") String token,
            Authentication authentication) {

        String chavePublica = (String) authentication.getPrincipal();
        log.info("Requisicao para registrar venda - chavePublica: {}, codigosRfid: {}", chavePublica, dto.codigosRfid().size());
        embarcadoService.registrarVenda(chavePublica, dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/produto")
    @Operation(summary = "Cadastrar produto", description = "Cadastra um novo produto via dispositivo de cadastro")
    public ResponseEntity<Void> cadastrarProduto(
            @RequestBody @Valid CadastrarProdutoRequest dto,
            @Parameter(description = "Token de sessao obtido no /auth", required = true)
            @RequestHeader("X-Embarcado-Token") String token,
            Authentication authentication) {

        String chavePublica = (String) authentication.getPrincipal();
        log.info("Requisicao para cadastrar produto - chavePublica: {}, rfid: {}", chavePublica, dto.rfid());
        embarcadoService.cadastrarProduto(chavePublica, dto);
        return ResponseEntity.ok().build();
    }
}
