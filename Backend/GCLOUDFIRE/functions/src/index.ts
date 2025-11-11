import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// 1. INICIALIZAR O FIREBASE ADMIN PRIMEIRO
admin.initializeApp();

// 2. AGORA IMPORTAR O RESTO
import express from 'express';
import cors from 'cors';
// (Não precisamos mais de swaggerUi, YAML, ou path aqui)

// (Os nossos módulos)
import mainRouter from './routes';

// Criar app Express
const app = express();

// === Middlewares ===
app.use(cors({ origin: true }));
app.use(express.json());

// === Rotas da API ===
// (O 'mainRouter' agora controla /apoiados E /docs)
app.use('/', mainRouter);

// 3. O bloco try...catch do Swagger foi REMOVIDO daqui

// Exportar a app Express como uma Firebase Function
export const api = functions.https.onRequest(app);