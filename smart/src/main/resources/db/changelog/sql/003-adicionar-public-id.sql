-- ============================================================
-- ADICIONAR PUBLIC_ID (UUID) NAS TABELAS
-- ============================================================

-- Habilitar extensão UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------
-- 1. ADMINISTRADORES - Adicionar public_id
-- ------------------------------------------------------------
ALTER TABLE administradores ADD COLUMN public_id UUID NOT NULL DEFAULT uuid_generate_v4();
CREATE UNIQUE INDEX uk_admin_public_id ON administradores (public_id);

-- ------------------------------------------------------------
-- 2. ENDERECOS - Adicionar public_id
-- ------------------------------------------------------------
ALTER TABLE enderecos ADD COLUMN public_id UUID NOT NULL DEFAULT uuid_generate_v4();
CREATE UNIQUE INDEX uk_endereco_public_id ON enderecos (public_id);

-- ------------------------------------------------------------
-- 3. LOJAS - Adicionar public_id
-- ------------------------------------------------------------
ALTER TABLE lojas ADD COLUMN public_id UUID NOT NULL DEFAULT uuid_generate_v4();
CREATE UNIQUE INDEX uk_loja_public_id ON lojas (public_id);
