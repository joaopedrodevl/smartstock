-- Seed do primeiro administrador (joao@email.com / 123456)
INSERT INTO administradores (nome, telefone, email, senha_hash, ativo)
VALUES ('João Admin', '83988881222', 'joao@email.com', '$2a$10$Qzuif3BCRwNeEViSn4tALuYnpcAOTQMZiL0ZKhO3PJD3y4HCbfCOG', true)
ON CONFLICT (email) DO NOTHING;
