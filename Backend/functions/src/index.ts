/*import * as functions from 'firebase-functions';
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
export const api = functions.https.onRequest(app);*/

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
// CORREÇÃO: Importar 'express' e 'cors' como exportações padrão (sem o * as)
import express from "express";
import cors from "cors";
import routes from "./routes";

// Inicializa a app do Firebase Admin
admin.initializeApp();

// Exportamos a instância da base de dados (db) para ser usada nos controladores
export const db = admin.firestore();

const app = express();

// Permite pedidos de qualquer origem (CORS)
app.use(cors({ origin: true }));

// Define o prefixo /api para todas as rotas
app.use("/api", routes);

// Exporta a função 'api' para o Firebase Cloud Functions
export const api = functions.https.onRequest(app);