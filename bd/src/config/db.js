const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false // Obrigatório para conexões com Supabase / Neon / Render
  }
});

pool.on('connect', () => {
  console.log('✓ Conectado ao banco de dados PostgreSQL');
});

pool.on('error', (err) => {
  console.error('Erro no pool do banco de dados:', err);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};