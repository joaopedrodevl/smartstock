-- Adiciona public_id na tabela produtos
ALTER TABLE produtos ADD COLUMN public_id uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE produtos ADD CONSTRAINT produtos_public_id_key UNIQUE (public_id);

-- Migrar valores monetários de numeric(10,2) para bigint (centavos)
UPDATE produtos SET preco = preco * 100;
ALTER TABLE produtos ALTER COLUMN preco TYPE bigint USING preco::bigint;
ALTER TABLE produtos ALTER COLUMN preco SET NOT NULL;

UPDATE vendas SET valor_total = valor_total * 100;
ALTER TABLE vendas ALTER COLUMN valor_total TYPE bigint USING valor_total::bigint;
ALTER TABLE vendas ALTER COLUMN valor_total SET NOT NULL;

UPDATE historico_vendas SET preco_unitario = preco_unitario * 100;
ALTER TABLE historico_vendas ALTER COLUMN preco_unitario TYPE bigint USING preco_unitario::bigint;
ALTER TABLE historico_vendas ALTER COLUMN preco_unitario SET NOT NULL;

UPDATE itens_venda SET preco_unitario = preco_unitario * 100, subtotal = subtotal * 100;
ALTER TABLE itens_venda ALTER COLUMN preco_unitario TYPE bigint USING preco_unitario::bigint;
ALTER TABLE itens_venda ALTER COLUMN preco_unitario SET NOT NULL;
ALTER TABLE itens_venda ALTER COLUMN subtotal TYPE bigint USING subtotal::bigint;
ALTER TABLE itens_venda ALTER COLUMN subtotal SET NOT NULL;
