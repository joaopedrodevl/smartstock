const express = require('express');
const router = express.Router();
const estoqueController = require('../controllers/estoqueController');

// GET /api/estoque -> Retorna o histórico diretamente
router.get('/', estoqueController.obterHistorico);
router.get('/historico', estoqueController.obterHistorico);
router.post('/movimentacao', estoqueController.registrarMovimentacao);

module.exports = router;