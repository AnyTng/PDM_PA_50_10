import * as admin from "firebase-admin";

// Inicializa a app apenas se ainda não existir nenhuma instância
// Removemos o 'serviceAccount' local para garantir segurança em produção e no git
if (!admin.apps.length) {
  admin.initializeApp(); 
}

const db = admin.firestore();

export { admin, db };