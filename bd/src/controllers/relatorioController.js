const db = require('../config/db');
const { Parser } = require('json2csv');
const PDFDocument = require('pdfkit');

// Exportação de Produtos e Estoque Atual em formato CSV
exports.exportarCSV = async (req, res) => {
  try {
    const sql = `
      SELECT 
        p.id AS codigo_rfid, 
        p.nome, 
        p.categoria, 
        p.estoque_minimo, 
        p.preco,
        COALESCE(SUM(CASE 
          WHEN m.tipo_movimentacao = 'ENTRADA' THEN m.quantidade 
          WHEN m.tipo_movimentacao IN ('SAIDA', 'PERDA') THEN -m.quantidade 
          ELSE 0 
        END), 0) AS estoque_atual
      FROM produtos p
      LEFT JOIN movimentacoes m ON p.id = m.produto_id
      GROUP BY p.id, p.nome, p.categoria, p.estoque_minimo, p.preco
      ORDER BY p.nome ASC;
    `;

    const result = await db.query(sql);

    const fields = ['codigo_rfid', 'nome', 'categoria', 'estoque_minimo', 'preco', 'estoque_atual'];
    const json2csvParser = new Parser({ fields, delimiter: ';' });
    const csv = json2csvParser.parse(result.rows);

    res.setHeader('Content-Type', 'text/csv');
    res.setHeader('Content-Disposition', 'attachment; filename=relatorio-estoque.csv');
    return res.status(200).send(csv);
  } catch (error) {
    console.error('Erro na exportação CSV:', error);
    return res.status(500).json({ error: 'Erro ao gerar arquivo CSV.', detalhe: error.message });
  }
};

// Exportação de Relatório Gerencial em PDF
exports.exportarPDF = async (req, res) => {
  try {
    const sql = `
      SELECT 
        p.id AS codigo_rfid, 
        p.nome, 
        p.categoria, 
        p.estoque_minimo, 
        p.preco,
        COALESCE(SUM(CASE 
          WHEN m.tipo_movimentacao = 'ENTRADA' THEN m.quantidade 
          WHEN m.tipo_movimentacao IN ('SAIDA', 'PERDA') THEN -m.quantidade 
          ELSE 0 
        END), 0) AS estoque_atual
      FROM produtos p
      LEFT JOIN movimentacoes m ON p.id = m.produto_id
      GROUP BY p.id, p.nome, p.categoria, p.estoque_minimo, p.preco
      ORDER BY p.nome ASC;
    `;

    const result = await db.query(sql);

    const doc = new PDFDocument({ margin: 30 });

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', 'attachment; filename=relatorio-smartstock.pdf');

    doc.pipe(res);

    doc.fontSize(18).text('SmartStock - Relatório Gerencial de Estoque', { align: 'center' });
    doc.moveDown(0.5);
    doc.fontSize(10).text(`Gerado em: ${new Date().toLocaleString('pt-BR')}`, { align: 'center' });
    doc.moveDown(1.5);

    doc.fontSize(11).font('Helvetica-Bold').text('Código RFID | Nome do Produto | Preço | Estoque Atual | Est. Mínimo');
    doc.text('-----------------------------------------------------------------------------------------');
    doc.font('Helvetica');

    result.rows.forEach((p) => {
      doc.fontSize(10).text(`${p.codigo_rfid} | ${p.nome} | R$ ${Number(p.preco).toFixed(2)} | Qtd: ${p.estoque_atual} | Mín: ${p.estoque_minimo}`);
    });

    doc.end();
  } catch (error) {
    console.error('Erro na exportação PDF:', error);
    return res.status(500).json({ error: 'Erro ao gerar arquivo PDF.', detalhe: error.message });
  }
};