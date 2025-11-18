/*import * as functions from "firebase-functions";
import express from "express";
import cors from "cors";
import routes from "./routes";

// --- CORREÇÃO CRÍTICA ---
// Removemos a inicialização do admin aqui.
// O admin.initializeApp() agora acontece EXCLUSIVAMENTE em 'src/config/firebase.ts'
// que é chamado automaticamente quando os controladores são carregados.

const app = express();

// Configura o CORS para permitir pedidos de qualquer origem
app.use(cors({ origin: true }));

// Configura o parser de JSON
app.use(express.json());

// Regista as rotas com o prefixo /api
app.use("/api", routes);

// Exporta a função para o Firebase
export const api = functions.https.onRequest(app);*/
import * as functions from "firebase-functions";
import express from "express";
import cors from "cors";
import routes from "./routes";

// --- ATENÇÃO ---
// Este ficheiro NÃO pode ter 'admin.initializeApp()'.
// Se tiveres essa linha aqui, apaga-a!
// A inicialização é feita APENAS no ficheiro 'config/firebase.ts'.

const app = express();

app.use(cors({ origin: true }));
app.use(express.json());
app.use("/api", routes);

export const api = functions.https.onRequest(app);