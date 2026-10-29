const db = require('../config/db');

// Cadastrar novo produto no catálogo
exports.criarProduto = async (req, res) => {
  const { id, nome, categoria, estoque_minimo, preco } = req.body;

  try {
    const result = await db.query(
      `INSERT INTO produtos (id, nome, categoria, estoque_minimo, preco)
       VALUES ($1, $2, $3, $4, $5) RETURNING *`,
      [id, nome, categoria, estoque_minimo || 5, preco]
    );
    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (error) {
    console.error('Erro ao cadastrar produto:', error);
    res.status(500).json({ error: 'Erro ao cadastrar produto', detalhe: error.message });
  }
};

// Listar todos os produtos
exports.listarProdutos = async (req, res) => {
  try {
    const result = await db.query('SELECT * FROM produtos ORDER BY nome ASC');
    res.json(result.rows);
  } catch (error) {
    console.error('Erro ao listar produtos:', error); // <-- Exibe a falha real no terminal
    res.status(500).json({ error: 'Erro ao listar produtos', detalhe: error.message });
  }
};

// Retornar produtos com estoque abaixo ou igual ao mínimo estipulado (considera ENTRADA, SAIDA e PERDA)
exports.obterAlertasEstoque = async (req, res) => {
  try {
    const query = `
      SELECT p.id, p.nome, p.estoque_minimo,
             COALESCE(SUM(CASE 
               WHEN m.tipo_movimentacao = 'ENTRADA' THEN m.quantidade 
               WHEN m.tipo_movimentacao IN ('SAIDA', 'PERDA') THEN -m.quantidade 
               ELSE 0 
             END), 0) AS estoque_atual
      FROM produtos p
      LEFT JOIN movimentacoes m ON p.id = m.produto_id
      GROUP BY p.id, p.nome, p.estoque_minimo
      HAVING COALESCE(SUM(CASE 
               WHEN m.tipo_movimentacao = 'ENTRADA' THEN m.quantidade 
               WHEN m.tipo_movimentacao IN ('SAIDA', 'PERDA') THEN -m.quantidade 
               ELSE 0 
             END), 0) <= p.estoque_minimo;
    `;
    const result = await db.query(query);
    res.json(result.rows);
  } catch (error) {
    console.error('Erro ao consultar alertas:', error);
    res.status(500).json({ error: 'Erro ao consultar alertas de estoque', detalhe: error.message });
  }
};