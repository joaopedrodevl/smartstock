-- ============================================================
-- TABELA DE EMBARCADOS (ESP32)
-- ============================================================

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

-- Garante no maximo 1 embarcado de cada tipo por loja
CREATE UNIQUE INDEX uk_embarcado_tipo_por_loja ON embarcados (loja_id, tipo);

-- Indices para buscas frequentes
CREATE INDEX idx_embarcado_chave ON embarcados (chave_publica);
CREATE INDEX idx_embarcado_loja ON embarcados (loja_id);

CREATE TRIGGER trg_embarcado_atualizado
    BEFORE UPDATE ON embarcados
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();
