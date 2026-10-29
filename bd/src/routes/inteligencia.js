const express = require('express');
const router = express.Router();
const inteligenciaController = require('../controllers/inteligenciaController');

router.get('/mais-vendidos', inteligenciaController.getProdutosMaisVendidos);
router.get('/sugestoes-reposicao', inteligenciaController.getSugestoesReposicao);
router.get('/validade', inteligenciaController.getAlertasValidade);
router.get('/historico', inteligenciaController.getHistoricoCompleto);

module.exports = router;