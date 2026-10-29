const express = require('express');
const router = express.Router();
const relatorioController = require('../controllers/relatorioController');

router.get('/exportar/csv', relatorioController.exportarCSV);
router.get('/exportar/pdf', relatorioController.exportarPDF);

module.exports = router;