-- Tabela de Produtos
CREATE TABLE IF NOT EXISTS produtos (
    id VARCHAR(50) PRIMARY KEY, -- Código de barras ou Tag RFID
    nome VARCHAR(100) NOT NULL,
    categoria VARCHAR(50),
    estoque_minimo INT DEFAULT 5,
    preco DECIMAL(10,2) NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Lotes (para controle de validade)
CREATE TABLE IF NOT EXISTS lotes (
    id SERIAL PRIMARY KEY,
    produto_id VARCHAR(50) REFERENCES produtos(id) ON DELETE CASCADE,
    quantidade INT NOT NULL,
    data_validade DATE NOT NULL,
    data_entrada TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Movimentações (alimentada pelo ESP32 e pela Interface Web)
CREATE TABLE IF NOT EXISTS movimentacoes (
    id SERIAL PRIMARY KEY,
    produto_id VARCHAR(50) REFERENCES produtos(id) ON DELETE CASCADE,
    tipo_movimentacao VARCHAR(10) CHECK (tipo_movimentacao IN ('ENTRADA', 'SAIDA', 'PERDA')),
    quantidade INT NOT NULL,
    dispositivo_id VARCHAR(50) DEFAULT 'MANUAL',
    data_movimentacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);