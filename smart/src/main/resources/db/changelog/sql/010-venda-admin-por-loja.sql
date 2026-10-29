-- Vendedor (ROLE_VENDA_ADMIN): vincula um administrador a uma loja
ALTER TABLE administradores ADD COLUMN loja_id BIGINT REFERENCES lojas(id) ON DELETE RESTRICT;

-- Garante um único vendedor por loja
CREATE UNIQUE INDEX idx_admin_venda_por_loja
    ON administradores (loja_id)
    WHERE role = 'ROLE_VENDA_ADMIN';
