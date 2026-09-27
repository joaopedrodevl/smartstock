-- Adiciona public_id a tabela vendas
ALTER TABLE vendas ADD COLUMN public_id UUID UNIQUE DEFAULT gen_random_uuid();
UPDATE vendas SET public_id = gen_random_uuid() WHERE public_id IS NULL;
ALTER TABLE vendas ALTER COLUMN public_id SET NOT NULL;
