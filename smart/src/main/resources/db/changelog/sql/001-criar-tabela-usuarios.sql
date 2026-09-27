-- ============================================================
-- MINI MERCADO AUTÔNOMO - SCHEMA POSTGRESQL
-- ============================================================

-- ------------------------------------------------------------
-- 0. TIPOS ENUM
-- ------------------------------------------------------------
CREATE TYPE forma_pagamento_enum AS ENUM ('dinheiro', 'pix', 'debito', 'credito', 'outro');
CREATE TYPE status_venda_enum    AS ENUM ('aberta', 'finalizada', 'cancelada');


-- ------------------------------------------------------------
-- 1. FUNÇÃO GENÉRICA PARA atualizado_em
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_atualizado_em()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ------------------------------------------------------------
-- 2. ADMINISTRADORES
-- ------------------------------------------------------------
CREATE TABLE administradores (
                                 id            BIGSERIAL      PRIMARY KEY,
                                 nome          VARCHAR(150)   NOT NULL,
                                 telefone      VARCHAR(20)    NOT NULL,
                                 email         VARCHAR(180)   NOT NULL UNIQUE,
                                 senha_hash    VARCHAR(255)   NOT NULL,
                                 ativo         BOOLEAN        NOT NULL DEFAULT TRUE,
                                 ultimo_login  TIMESTAMP      NULL,
                                 criado_em     TIMESTAMP      NOT NULL DEFAULT NOW(),
                                 atualizado_em TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_ativo ON administradores (ativo);

CREATE TRIGGER trg_admin_atualizado
    BEFORE UPDATE ON administradores
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();


-- ------------------------------------------------------------
-- 3. ENDERECOS
-- ------------------------------------------------------------
CREATE TABLE enderecos (
                           id            BIGSERIAL      PRIMARY KEY,
                           cep           VARCHAR(8)     NOT NULL,
                           logradouro    VARCHAR(180)   NOT NULL,
                           numero        VARCHAR(20)    NOT NULL,
                           complemento   VARCHAR(100)   NULL,
                           bairro        VARCHAR(100)   NOT NULL,
                           cidade        VARCHAR(100)   NOT NULL,
                           estado        CHAR(2)        NOT NULL,
                           referencia    VARCHAR(180)   NULL,
                           criado_em     TIMESTAMP      NOT NULL DEFAULT NOW(),
                           atualizado_em TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_endereco_cep       ON enderecos (cep);
CREATE INDEX idx_endereco_cidade_uf ON enderecos (cidade, estado);

CREATE TRIGGER trg_endereco_atualizado
    BEFORE UPDATE ON enderecos
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();


-- ------------------------------------------------------------
-- 4. LOJAS
-- ------------------------------------------------------------
CREATE TABLE lojas (
                       id                BIGSERIAL      PRIMARY KEY,
                       administrador_id  BIGINT         NOT NULL REFERENCES administradores(id) ON DELETE RESTRICT,
                       endereco_id       BIGINT         NOT NULL REFERENCES enderecos(id)       ON DELETE RESTRICT,
                       cnpj              VARCHAR(14)    NOT NULL UNIQUE,
                       nome_fantasia     VARCHAR(150)   NOT NULL,
                       razao_social      VARCHAR(200)   NULL,
                       telefone          VARCHAR(20)    NULL,
                       eh_matriz         BOOLEAN        NOT NULL DEFAULT FALSE,
                       ativo             BOOLEAN        NOT NULL DEFAULT TRUE,
                       criado_em         TIMESTAMP      NOT NULL DEFAULT NOW(),
                       atualizado_em     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loja_admin    ON lojas (administrador_id);
CREATE INDEX idx_loja_endereco ON lojas (endereco_id);

-- Garante uma única matriz por administrador
CREATE UNIQUE INDEX uk_loja_matriz_unica
    ON lojas (administrador_id)
    WHERE eh_matriz = TRUE;

CREATE TRIGGER trg_loja_atualizado
    BEFORE UPDATE ON lojas
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();


-- ------------------------------------------------------------
-- 5. CLIENTES
-- ------------------------------------------------------------
CREATE TABLE clientes (
                          id            BIGSERIAL      PRIMARY KEY,
                          cpf           VARCHAR(11)    NOT NULL UNIQUE,
                          telefone      VARCHAR(20)    NOT NULL,
                          nome          VARCHAR(150)   NULL,
                          criado_em     TIMESTAMP      NOT NULL DEFAULT NOW(),
                          atualizado_em TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cliente_telefone ON clientes (telefone);

CREATE TRIGGER trg_cliente_atualizado
    BEFORE UPDATE ON clientes
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();


-- ------------------------------------------------------------
-- 6. REFRESH TOKENS (JWT do admin)
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
                                id               BIGSERIAL      PRIMARY KEY,
                                administrador_id BIGINT         NOT NULL REFERENCES administradores(id) ON DELETE CASCADE,
                                token_hash       VARCHAR(255)   NOT NULL UNIQUE,
                                expira_em        TIMESTAMP      NOT NULL,
                                revogado         BOOLEAN        NOT NULL DEFAULT FALSE,
                                user_agent       VARCHAR(255)   NULL,
                                ip               VARCHAR(45)    NULL,
                                criado_em        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_admin ON refresh_tokens (administrador_id);


-- ------------------------------------------------------------
-- 7. PRODUTOS
-- ------------------------------------------------------------
CREATE TABLE produtos (
                          id            BIGSERIAL      PRIMARY KEY,
                          loja_id       BIGINT         NOT NULL REFERENCES lojas(id) ON DELETE RESTRICT,
                          codigo_barras VARCHAR(50)    NULL,
                          nome          VARCHAR(150)   NOT NULL,
                          descricao     VARCHAR(255)   NULL,
                          preco         NUMERIC(10,2)  NOT NULL,
                          estoque       INT            NOT NULL DEFAULT 0 CHECK (estoque >= 0),
                          ativo         BOOLEAN        NOT NULL DEFAULT TRUE,
                          criado_em     TIMESTAMP      NOT NULL DEFAULT NOW(),
                          atualizado_em TIMESTAMP      NOT NULL DEFAULT NOW(),
                          UNIQUE (loja_id, codigo_barras)
);

CREATE INDEX idx_produto_loja ON produtos (loja_id);

CREATE TRIGGER trg_produto_atualizado
    BEFORE UPDATE ON produtos
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();


-- ------------------------------------------------------------
-- 8. VENDAS
-- ------------------------------------------------------------
CREATE TABLE vendas (
                        id              BIGSERIAL             PRIMARY KEY,
                        loja_id         BIGINT                NOT NULL REFERENCES lojas(id)    ON DELETE RESTRICT,
                        cliente_id      BIGINT                NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT,
                        valor_total     NUMERIC(10,2)         NOT NULL DEFAULT 0,
                        forma_pagamento forma_pagamento_enum  NOT NULL DEFAULT 'pix',
                        status          status_venda_enum     NOT NULL DEFAULT 'aberta',
                        observacao      VARCHAR(255)          NULL,
                        finalizada_em   TIMESTAMP             NULL,
                        criado_em       TIMESTAMP             NOT NULL DEFAULT NOW(),
                        atualizado_em   TIMESTAMP             NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_venda_loja    ON vendas (loja_id);
CREATE INDEX idx_venda_cliente ON vendas (cliente_id);
CREATE INDEX idx_venda_status  ON vendas (status);
CREATE INDEX idx_venda_criado  ON vendas (criado_em);

CREATE TRIGGER trg_venda_atualizado
    BEFORE UPDATE ON vendas
    FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();


-- ------------------------------------------------------------
-- 9. ITENS DE VENDA
-- ------------------------------------------------------------
CREATE TABLE itens_venda (
                             id              BIGSERIAL      PRIMARY KEY,
                             venda_id        BIGINT         NOT NULL REFERENCES vendas(id)   ON DELETE CASCADE,
                             produto_id      BIGINT         NOT NULL REFERENCES produtos(id) ON DELETE RESTRICT,
                             quantidade      INT            NOT NULL CHECK (quantidade > 0),
                             preco_unitario  NUMERIC(10,2)  NOT NULL,
                             subtotal        NUMERIC(10,2)  NOT NULL,
                             criado_em       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_item_venda   ON itens_venda (venda_id);
CREATE INDEX idx_item_produto ON itens_venda (produto_id);
