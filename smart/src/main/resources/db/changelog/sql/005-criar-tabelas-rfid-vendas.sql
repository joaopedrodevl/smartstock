-- ============================================================
-- TABELAS DE RFID, VENDAS E HISTORICO
-- ============================================================

CREATE TYPE status_leitura_rfid_enum AS ENUM ('PENDENTE', 'LIDO', 'EXPIRADO', 'CANCELADO');

-- Tags RFID vinculadas a produtos
CREATE TABLE tags_rfid (
    id              BIGSERIAL      PRIMARY KEY,
    public_id       UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    loja_id         BIGINT         NOT NULL REFERENCES lojas(id) ON DELETE RESTRICT,
    produto_id      BIGINT         NOT NULL REFERENCES produtos(id) ON DELETE RESTRICT,
    codigo_rfid     VARCHAR(100)   NOT NULL UNIQUE,
    ativo           BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP      NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_tag_rfid_codigo ON tags_rfid (codigo_rfid);
CREATE INDEX idx_tag_rfid_produto ON tags_rfid (produto_id);
CREATE INDEX idx_tag_rfid_loja ON tags_rfid (loja_id);

CREATE TRIGGER trg_tag_rfid_atualizado
    BEFORE UPDATE ON tags_rfid
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();

-- Solicitacoes de leitura RFID (para vinculacao de produto)
CREATE TABLE leituras_rfid (
    id              BIGSERIAL                  PRIMARY KEY,
    public_id       UUID                       NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    loja_id         BIGINT                     NOT NULL REFERENCES lojas(id) ON DELETE RESTRICT,
    embarcado_key   VARCHAR(64)                NOT NULL,
    status          status_leitura_rfid_enum   NOT NULL DEFAULT 'PENDENTE',
    codigo_rfid     VARCHAR(100)               NULL,
    produto_id      BIGINT                     NULL REFERENCES produtos(id) ON DELETE SET NULL,
    criado_em       TIMESTAMP                  NOT NULL DEFAULT NOW(),
    expira_em       TIMESTAMP                  NOT NULL
);

CREATE INDEX idx_leitura_rfid_status ON leituras_rfid (status);
CREATE INDEX idx_leitura_rfid_embarcado ON leituras_rfid (embarcado_key);
CREATE INDEX idx_leitura_rfid_loja ON leituras_rfid (loja_id);

-- Historico de vendas (auditoria - dados mesmo apos desvincular RFID)
CREATE TABLE historico_vendas (
    id              BIGSERIAL      PRIMARY KEY,
    public_id       UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    loja_id         BIGINT         NOT NULL REFERENCES lojas(id) ON DELETE RESTRICT,
    produto_id      BIGINT         NOT NULL REFERENCES produtos(id) ON DELETE RESTRICT,
    codigo_rfid     VARCHAR(100)   NOT NULL,
    preco_unitario  NUMERIC(10,2)  NOT NULL,
    vendido_em      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_historico_venda_loja ON historico_vendas (loja_id);
CREATE INDEX idx_historico_venda_produto ON historico_vendas (produto_id);
CREATE INDEX idx_historico_venda_rfid ON historico_vendas (codigo_rfid);
CREATE INDEX idx_historico_venda_data ON historico_vendas (vendido_em);
