-- Adiciona role a tabela administradores
ALTER TABLE administradores ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'ROLE_ADMIN';
