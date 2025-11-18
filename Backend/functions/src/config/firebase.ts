import * as admin from "firebase-admin";

// Inicializa a app apenas se ainda não estiver inicializada
// Isto previne erros de "App already exists"
if (!admin.apps.length) {
  admin.initializeApp();
}

// Exporta a base de dados para ser usada em qualquer outro ficheiro
export const db = admin.firestore();