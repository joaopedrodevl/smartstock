const db = require('../config/db');

// Registrar entrada ou saída (chamado pela ESP32 ou Web)
exports.registrarMovimentacao = async (req, res) => {
  const { produto_id, tipo_movimentacao, quantidade, dispositivo_id } = req.body;

  if (!produto_id || !tipo_movimentacao || !quantidade) {
    return res.status(400).json({ error: 'Campos obrigatórios: produto_id, tipo_movimentacao, quantidade' });
  }

  try {
    const result = await db.query(
      `INSERT INTO movimentacoes (produto_id, tipo_movimentacao, quantidade, dispositivo_id)
       VALUES ($1, $2, $3, $4) RETURNING *`,
      [produto_id, tipo_movimentacao.toUpperCase(), quantidade, dispositivo_id || 'ESP32_DEFAULT']
    );

    console.log(`[IoT LOG] Movimentação registrada: ${tipo_movimentacao} | Item: ${produto_id}`);
    res.status(201).json({ success: true, data: result.rows[0] });
  } catch (error) {
    console.error('Erro ao registrar movimentação:', error);
    res.status(500).json({ error: 'Erro ao registrar movimentação no banco' });
  }
};

// Consultar todo o histórico de movimentações
exports.obterHistorico = async (req, res) => {
  try {
    const result = await db.query(
      `SELECT m.*, p.nome as produto_nome 
       FROM movimentacoes m 
       LEFT JOIN produtos p ON m.produto_id = p.id 
       ORDER BY m.data_movimentacao DESC LIMIT 100`
    );
    res.json(result.rows);
  } catch (error) {
    console.error('Erro ao buscar histórico:', error);
    res.status(500).json({ error: 'Erro ao buscar histórico' });
  }
};