const db = require('../config/db');

// 1. Produtos Mais Vendidos (Rotatividade)
exports.getProdutosMaisVendidos = async (req, res) => {
  try {
    const dias = parseInt(req.query.dias) || 30;
    const limite = parseInt(req.query.limite) || 10;

    const sql = `
      SELECT 
        p.id, 
        p.nome, 
        p.categoria, 
        COALESCE(SUM(m.quantidade), 0) AS total_vendido,
        COUNT(m.id) AS total_operacoes
      FROM movimentacoes m
      JOIN produtos p ON m.produto_id = p.id
      WHERE m.tipo_movimentacao = 'SAIDA'
        AND m.data_movimentacao >= NOW() - ($1 || ' days')::INTERVAL
      GROUP BY p.id, p.nome, p.categoria
      ORDER BY total_vendido DESC
      LIMIT $2;
    `;

    const result = await db.query(sql, [dias, limite]);
    return res.json(result.rows);
  } catch (error) {
    console.error('Erro em getProdutosMaisVendidos:', error);
    return res.status(500).json({ error: 'Erro ao buscar produtos mais vendidos.', detalhe: error.message });
  }
};

// 2. Sugestões de Reposição de Estoque
exports.getSugestoesReposicao = async (req, res) => {
  try {
    const sql = `
      SELECT 
        p.id,
        p.nome,
        p.categoria,
        p.estoque_minimo,
        COALESCE(SUM(CASE 
          WHEN m.tipo_movimentacao = 'ENTRADA' THEN m.quantidade 
          WHEN m.tipo_movimentacao IN ('SAIDA', 'PERDA') THEN -m.quantidade 
          ELSE 0 
        END), 0) AS estoque_atual,
        GREATEST((p.estoque_minimo * 2) - COALESCE(SUM(CASE 
          WHEN m.tipo_movimentacao = 'ENTRADA' THEN m.quantidade 
          WHEN m.tipo_movimentacao IN ('SAIDA', 'PERDA') THEN -m.quantidade 
          ELSE 0 
        END), 0), 0) AS quantidade_sugerida_compra
      FROM produtos p
      LEFT JOIN movimentacoes m ON p.id = m.produto_id
      GROUP BY p.id, p.nome, p.categoria, p.estoque_minimo
      HAVING COALESCE(SUM(CASE 
        WHEN m.tipo_movimentacao = 'ENTRADA' THEN m.quantidade 
        WHEN m.tipo_movimentacao IN ('SAIDA', 'PERDA') THEN -m.quantidade 
        ELSE 0 
      END), 0) <= p.estoque_minimo
      ORDER BY estoque_atual ASC;
    `;

    const result = await db.query(sql);
    return res.json(result.rows);
  } catch (error) {
    console.error('Erro em getSugestoesReposicao:', error);
    return res.status(500).json({ error: 'Erro ao calcular sugestões de reposição.', detalhe: error.message });
  }
};

// 3. Alertas de Validade (Requisito F4 - Consulta na tabela 'lotes')
exports.getAlertasValidade = async (req, res) => {
  try {
    const dias = parseInt(req.query.dias) || 7;

    const sql = `
      SELECT 
        l.id AS lote_id,
        p.id AS produto_id,
        p.nome AS produto_nome,
        p.categoria,
        l.quantidade,
        l.data_validade,
        CASE 
          WHEN l.data_validade < CURRENT_DATE THEN 'VENCIDO'
          ELSE 'PROXIMO_AO_VENCIMENTO'
        END AS status_validade
      FROM lotes l
      JOIN produtos p ON l.produto_id = p.id
      WHERE l.data_validade <= CURRENT_DATE + ($1 || ' days')::INTERVAL
      ORDER BY l.data_validade ASC;
    `;

    const result = await db.query(sql, [dias]);
    return res.json(result.rows);
  } catch (error) {
    console.error('Erro em getAlertasValidade:', error);
    return res.status(500).json({ error: 'Erro ao consultar alertas de validade.', detalhe: error.message });
  }
};

// 4. Histórico Completo com Filtros e Paginação (Requisito F3)
exports.getHistoricoCompleto = async (req, res) => {
  try {
    const pagina = parseInt(req.query.pagina) || 1;
    const limite = parseInt(req.query.limite) || 20;
    const offset = (pagina - 1) * limite;
    const { tipo, produto_id } = req.query;

    let sql = `
      SELECT 
        m.id,
        m.produto_id,
        p.nome AS produto_nome,
        m.tipo_movimentacao,
        m.quantidade,
        m.dispositivo_id,
        m.data_movimentacao
      FROM movimentacoes m
      JOIN produtos p ON m.produto_id = p.id
      WHERE 1=1
    `;

    const params = [];

    if (tipo) {
      params.push(tipo.toUpperCase());
      sql += ` AND m.tipo_movimentacao = $${params.length}`;
    }

    if (produto_id) {
      params.push(produto_id);
      sql += ` AND m.produto_id = $${params.length}`;
    }

    params.push(limite);
    sql += ` ORDER BY m.data_movimentacao DESC LIMIT $${params.length}`;

    params.push(offset);
    sql += ` OFFSET $${params.length}`;

    const result = await db.query(sql, params);
    return res.json({
      pagina,
      limite,
      total_registros: result.rows.length,
      dados: result.rows
    });
  } catch (error) {
    console.error('Erro em getHistoricoCompleto:', error);
    return res.status(500).json({ error: 'Erro ao buscar histórico completo.', detalhe: error.message });
  }
};