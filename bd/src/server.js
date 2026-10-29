const express = require('express');
const cors = require('cors');
require('dotenv').config();

const estoqueRoutes = require('./routes/estoque');
const produtoRoutes = require('./routes/produtos');
const inteligenciaRoutes = require('./routes/inteligencia');
const relatoriosRoutes = require('./routes/relatorios');

const app = express();

app.use(cors());
app.use(express.json());

// Registro de Rotas da API
app.use('/api/estoque', estoqueRoutes);
app.use('/api/produtos', produtoRoutes);
app.use('/api/inteligencia', inteligenciaRoutes);
app.use('/api/relatorios', relatoriosRoutes);

// Rota raiz para verificação de status
app.get('/', (req, res) => {
  res.json({ status: 'API SmartStock online', timestamp: new Date() });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`🚀 Servidor SmartStock rodando na porta ${PORT}`);
});