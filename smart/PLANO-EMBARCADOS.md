# Plano: Sistema de Embarcados SmartStock

## Visao Geral

Cada loja tera **2 embarcados ESP32** (1 venda + 1 cadastro), autenticados via **HMAC-SHA256** com segredo gravado em eFuse do ESP32. A autenticacao usa timestamp para prevenir replay attacks.

---

## 1. Autenticacao - HMAC com eFuse

### Por que HMAC?

- ESP32 tem **aceleracao hardware** de HMAC-SHA256 via modulo eFuse
- Segredo nunca viaja pela rede - apenas o hash resultante
- Previne **replay attacks** via timestamp no payload
padrao da industria (Huawei IoTDA, AWS IoT Core)

### Fluxo de Autenticacao

```
ESP32 (eFuse com segredo)                    API (servidor)
       |                                           |
       |  1. POST /v1/embarcado/auth               |
       |  { chave_publica: "uuid-esp32" }          |
       | ----------------------------------------> |
       |                                           |  2. Busca embarcado por chave_publica
       |                                           |     Retorna hmac_secret
       |  3. Resposta: { token_sessao, expiry }    |
       | <----------------------------------------- |
       |                                           |
       |  4. POST /v1/embarcado/venda              |
       |  Header: X-Embarcado-Token: {token}       |
       |  Body: { timestamp, dados }               |
       |  Signature: HMAC-SHA256(body, secret)     |
       | ----------------------------------------> |
       |                                           |  5. Valida timestamp (janela 5min)
       |                                           |  6. Recalcula HMAC, compara
       |  7. Resposta: { status: "ok" }            |
       | <----------------------------------------- |
```

### Regras de Seguranca

- Timestamp deve estar dentro de janela de **+/-5 minutos**
- HMAC calculado sobre: `timestamp + body`
- Token de sessao expira apos **30 minutos** - dispositivo deve renovar
- Segredo armazenado criptografado no banco

---

## 2. Migration SQL

**Arquivo:** `src/main/resources/db/changelog/sql/004-criar-tabela-embarcados.sql`

```sql
CREATE TYPE tipo_embarcado_enum AS ENUM ('VENDA', 'CADASTRO');

CREATE TABLE embarcados (
    id              BIGSERIAL      PRIMARY KEY,
    public_id       UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    loja_id         BIGINT         NOT NULL REFERENCES lojas(id) ON DELETE RESTRICT,
    chave_publica   VARCHAR(64)    NOT NULL UNIQUE,
    hmac_secret     VARCHAR(128)   NOT NULL,
    tipo            tipo_embarcado_enum NOT NULL,
    descricao       VARCHAR(150)   NULL,
    ativo           BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP      NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_embarcado_tipo_por_loja ON embarcados (loja_id, tipo);
CREATE INDEX idx_embarcado_chave ON embarcados (chave_publica);
CREATE INDEX idx_embarcado_loja ON embarcados (loja_id);

CREATE TRIGGER trg_embarcado_atualizado
    BEFORE UPDATE ON embarcados
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();
```

Atualizar `db.changelog-master.yaml` para incluir a migration 004.

---

## 3. Entidades de Dominio

### Enum TipoEmbarcado

**Arquivo:** `core/enums/TipoEmbarcado.java`

```java
package br.com.smartstock.smartstock.core.enums;

public enum TipoEmbarcado {
    VENDA,
    CADASTRO
}
```

### Entidade Embarcado

**Arquivo:** `core/entities/Embarcado.java`

```java
package br.com.smartstock.smartstock.core.entities;

import br.com.smartstock.smartstock.core.enums.TipoEmbarcado;
import java.time.LocalDateTime;
import java.util.UUID;

public class Embarcado {
    private Long id;
    private UUID publicId;
    private Long lojaId;
    private String chavePublica;
    private String hmacSecret;
    private TipoEmbarcado tipo;
    private String descricao;
    private Boolean ativo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
```

---

## 4. Ports (Repositorios)

**Arquivo:** `core/ports/EmbarcadoRepository.java`

```java
package br.com.smartstock.smartstock.core.ports;

import br.com.smartstock.smartstock.core.entities.Embarcado;
import br.com.smartstock.smartstock.core.enums.TipoEmbarcado;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmbarcadoRepository {
    Optional<Embarcado> findByChavePublica(String chavePublica);
    Optional<Embarcado> findByPublicId(UUID publicId);
    List<Embarcado> findByLojaId(Long lojaId);
    Embarcado save(Embarcado embarcado);
    void deleteByPublicId(UUID publicId);
    boolean existsByLojaIdAndTipo(Long lojaId, TipoEmbarcado tipo);
}
```

---

## 5. Implementacao JDBC

**Arquivo:** `infra/repositories/EmbarcadoRepositoryImpl.java`

- `RowMapper` manual seguindo padrao do projeto
- Query `findByChavePublica` para autenticacao
- Constraint `uk_embarcado_tipo_por_loja` controla max 2 por loja
- `hmac_secret` armazenado com hash (BCrypt ou similar)

---

## 6. DTOs

**Pacote:** `infra/dtos/embarcado/`

| DTO | Campos | Uso |
|-----|--------|-----|
| `CreateEmbarcadoRequest` | `lojaId`, `tipo`, `descricao` | Admin cadastra - chave e hmac_secret gerados pelo backend |
| `EmbarcadoResponse` | `publicId`, `lojaId`, `chavePublica`, `tipo`, `descricao`, `ativo` | Resposta para admin |
| `AuthEmbarcadoRequest` | `chavePublica` | ESP32 solicita token de sessao |
| `AuthEmbarcadoResponse` | `tokenSessao`, `expiraEm` | Resposta com token temporario |
| `RegistrarVendaRequest` | `itens: [{ rfid, quantidade }]` | Embarcado VENDA envia venda |
| `CadastrarProdutoRequest` | `rfid`, `nome`, `descricao`, `preco` | Embarcado CADASTRO cadastra produto |

---

## 7. Filtro de Autenticacao

**Arquivo:** `infra/filters/EmbarcadoAuthFilter.java`

```
1. Intercepta requests para /v1/embarcado/**
2. Extrai header X-Embarcado-Token
3. Decodifica token -> extrai chave_publica e expiracao
4. Valida expiracao
5. Busca embarcado no banco
6. Se valido: cria Authentication(ROLE_EMBARCADO + ROLE_VENDA ou ROLE_CADASTRO)
7. Se invalido: retorna 401
```

---

## 8. Servicos

### EmbarcadoAuthService

**Arquivo:** `application/services/EmbarcadoAuthService.java`

```java
public class EmbarcadoAuthService {
    // Gera token de sessao (JWT simples com chave_publica + expiracao)
    AuthEmbarcadoResponse autenticar(String chavePublica);

    // Valida token e retorna embarcado
    Optional<Embarcado> validarToken(String token);
}
```

### EmbarcadoService

**Arquivo:** `application/services/EmbarcadoService.java`

```java
public class EmbarcadoService {
    // Admin operations
    Embarcado cadastrar(Long lojaId, TipoEmbarcado tipo, String descricao);
    List<Embarcado> listarPorLoja(Long lojaId);
    Embarcado buscarPorPublicId(UUID publicId);
    void remover(UUID publicId);

    // Embarcado operations (ja autenticado)
    EmbarcadoHeartbeatResponse heartbeat(String chavePublica);
    void registrarVenda(String chavePublica, RegistrarVendaRequest request);
    void cadastrarProduto(String chavePublica, CadastrarProdutoRequest request);
}
```

**Validacoes de negocio:**
- `cadastrar`: verificar que a loja existe e nao ja existe o tipo para aquela loja
- `registrarVenda`: verificar que o embarcado e tipo VENDA, que os RFIDs existem na loja, debitar estoque
- `cadastrarProduto`: verificar que o embarcado e tipo CADASTRO

---

## 9. Controllers

### Controller Admin

**Arquivo:** `infra/controllers/v1/admin/EmbarcadoAdminController.java`

| Metodo | Endpoint | Auth | Descricao |
|--------|----------|------|-----------|
| POST | `/v1/admin/embarcados` | ROLE_ADMIN | Cadastra embarcado (gera chave + hmac_secret) |
| GET | `/v1/admin/embarcados?lojaPublicId=...` | ROLE_ADMIN | Lista embarcados da loja |
| GET | `/v1/admin/embarcados/{publicId}` | ROLE_ADMIN | Busca embarcado |
| DELETE | `/v1/admin/embarcados/{publicId}` | ROLE_ADMIN | Remove embarcado |

### Controller Embarcado

**Arquivo:** `infra/controllers/v1/EmbarcadoController.java`

| Metodo | Endpoint | Auth | Descricao |
|--------|----------|------|-----------|
| POST | `/v1/embarcado/auth` | Public | Autentica embarcado (retorna token) |
| POST | `/v1/embarcado/heartbeat` | ROLE_EMBARCADO | Valida conexao |
| POST | `/v1/embarcado/venda` | ROLE_EMBARCADO + VENDA | Registra venda |
| POST | `/v1/embarcado/produto` | ROLE_EMBARCADO + CADASTRO | Cadastra produto |

---

## 10. Atualizacao do RestConfig

**Arquivo:** `infra/config/RestConfig.java`

Adicionar no `SecurityFilterChain`:

```java
.requestMatchers("/v1/embarcado/auth").permitAll()
.requestMatchers("/v1/embarcado/**").hasAuthority("ROLE_EMBARCADO")
.addFilterBefore(embarcadoAuthFilter, UsernamePasswordAuthenticationFilter.class)
```

---

## 11. Arquivos a Criar/Modificar

| Acao | Arquivo |
|------|---------|
| Criar | `resources/db/changelog/sql/004-criar-tabela-embarcados.sql` |
| Editar | `resources/db/changelog/db.changelog-master.yaml` |
| Criar | `core/enums/TipoEmbarcado.java` |
| Criar | `core/entities/Embarcado.java` |
| Criar | `core/ports/EmbarcadoRepository.java` |
| Criar | `infra/repositories/EmbarcadoRepositoryImpl.java` |
| Criar | `infra/dtos/embarcado/CreateEmbarcadoRequest.java` |
| Criar | `infra/dtos/embarcado/EmbarcadoResponse.java` |
| Criar | `infra/dtos/embarcado/AuthEmbarcadoRequest.java` |
| Criar | `infra/dtos/embarcado/AuthEmbarcadoResponse.java` |
| Criar | `infra/dtos/embarcado/EmbarcadoHeartbeatResponse.java` |
| Criar | `infra/dtos/embarcado/RegistrarVendaRequest.java` |
| Criar | `infra/dtos/embarcado/CadastrarProdutoRequest.java` |
| Criar | `infra/filters/EmbarcadoAuthFilter.java` |
| Criar | `application/services/EmbarcadoAuthService.java` |
| Criar | `application/services/EmbarcadoService.java` |
| Criar | `infra/controllers/v1/admin/EmbarcadoAdminController.java` |
| Criar | `infra/controllers/v1/EmbarcadoController.java` |
| Editar | `infra/config/RestConfig.java` |

---

## 12. Ordem de Implementacao

1. Migration SQL (tabela embarcados + enum)
2. Entidade Embarcado + Enum TipoEmbarcado
3. Port EmbarcadoRepository
4. Implementacao JDBC EmbarcadoRepositoryImpl
5. DTOs (todos os records)
6. EmbarcadoService (logica de negocio)
7. EmbarcadoAuthService (autenticacao)
8. EmbarcadoAuthFilter (filtro de seguranca)
9. EmbarcadoAdminController (CRUD admin)
10. EmbarcadoController (endpoints do dispositivo)
11. Atualizar RestConfig (novas rotas + filtro)
12. Testes unitarios e de integracao
