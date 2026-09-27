# SmartStock — Ambiente de Desenvolvimento

Sistema de gestão de estoque/vendas para mini mercados de condomínio, composto por:

- **`smart/`** — backend (Spring Boot 4, Java 25, Gradle, PostgreSQL, Liquibase, WebSocket)
- **`smart-frontend/`** — frontend (React 19 + TypeScript + Vite 8)
- **`simula-embarcado.py`** — simulador de dispositivos embarcados (ESP32) para testes

---

## 1. Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| JDK | 25 | O Gradle baixa automaticamente via plugin Foojay se não estiver instalado |
| Docker | qualquer | Roda o banco PostgreSQL |
| Bun (ou Node ≥ 20) | — | Bun é o gerenciador usado no frontend (`bun.lock`) |
| Python 3 | ≥ 3.8 | Apenas para o simulador de embarcados |

---

## 2. Banco de dados (PostgreSQL)

O backend espera um PostgreSQL em `localhost:5432` com banco `mydatabase`, usuário `myuser` e senha `secret`.

```bash
cd smart
docker compose up -d
```

> O container criado será `smart-postgres-1` — o simulador de embarcados depende desse nome.

---

## 3. Backend (`smart/`)

### 3.1 Configuração das chaves RSA (JWT)

A autenticação usa JWT assinado com RSA. As chaves são carregadas do **classpath** (`src/main/resources/`) via propriedades em `application.properties`:

```properties
jwt.private.key: classpath:app.key
jwt.public.key: classpath:app.pub
```

Os arquivos `app.key` e `app.pub` já existem no repositório para desenvolvimento. Para **gerar um novo par de chaves** (recomendado se forem expiradas ou se você quiser chaves próprias):

```bash
# Gera a chave privada RSA 2048 bits no formato PKCS#8 (exigido pelo Spring)
openssl genpkey -algorithm RSA -out app.key -pkeyopt rsa_keygen_bits:2048

# Extrai a chave pública no formato PEM (SubjectPublicKeyInfo)
openssl pkey -in app.key -pubout -out app.pub
```

Coloque ambos em `smart/src/main/resources/`, respeitando o formato:

- `app.key` → `-----BEGIN PRIVATE KEY-----` (PKCS#8)
- `app.pub` → `-----BEGIN PUBLIC KEY-----` (PEM)

Elas são injetadas como `RSAPrivateKey`/`RSAPublicKey` em `infra/config/RestConfig.java` para configurar o `JwtEncoder`/`JwtDecoder` (Nimbus).

> ⚠️ As chaves do repositório são apenas para **desenvolvimento**. Nunca reutilize em produção e não versione chaves privadas reais.

### 3.2 Rodando

```bash
cd smart
./gradlew bootRun
```

- Sobe na porta **8080** (`http://localhost:8080`)
- As migrations do Liquibase rodam automaticamente no startup
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- WebSocket de embarcados: `ws://localhost:8080/ws/embarcado`

### 3.3 Usuário admin inicial (seed)

O migration `002-seed-admin-inicial.sql` cria um usuário admin:

```
email: joao@email.com
senha: 123456
```

Login via `POST /v1/token/admin` (retorna cookies `access_token` + `XSRF-TOKEN`).

### 3.4 Testes

```bash
./gradlew test
```

Usam H2 em memória + Testcontainers (PostgreSQL). As mesmas chaves RSA do classpath são usadas.

---

## 4. Frontend (`smart-frontend/`)

```bash
cd smart-frontend
bun install
bun run dev
```

- Sobe na porta **5173** (`http://localhost:5173`)
- O Vite já faz proxy de `/v1` → `http://localhost:8080` e `/ws` → `ws://localhost:8080`, além de ajustar os cookies de autenticação (remove `Secure`, aplica `SameSite=Lax`) — não precisa de `.env`

Outros comandos:

```bash
bun run lint     # Biome
bun run format   # formatar com Biome
bun run build    # tsc + build de produção
```

---

## 5. Simulador de embarcados (`simula-embarcado.py`)

Simula dispositivos ESP32: autentica via `POST /v1/embarcado/auth`, conecta ao WebSocket `/ws/embarcado` e responde a solicitações de leitura RFID. **Requer o backend rodando na porta 8080** e o container `smart-postgres-1` no ar.

```bash
pip install requests websockets
```

Uso:

```bash
# Dispositivo único (a chave precisa existir na tabela embarcados)
python3 simula-embarcado.py --chave-publica <uuid> --tipo VENDA

# Dois dispositivos padrão (VENDA e CADASTRO)
python3 simula-embarcado.py --all
```

Os embarcados são cadastrados via API admin (`/v1/admin/...`) — o UUID retornado é usado como `chavePublica` no simulador.

---

## 6. Ordem de inicialização

1. `docker compose up -d` (em `smart/`)
2. `./gradlew bootRun` (em `smart/`)
3. `bun run dev` (em `smart-frontend/`)
4. `python3 simula-embarcado.py --all` (opcional, para testar fluxo RFID)

## URLs úteis

| URL | Descrição |
|---|---|
| `http://localhost:5173` | Frontend |
| `http://localhost:8080/swagger-ui.html` | Swagger UI do backend |
| `http://localhost:8080/websocket-test.html` | Página estática de teste do WebSocket |
| `ws://localhost:8080/ws/embarcado` | WebSocket dos embarcados |
