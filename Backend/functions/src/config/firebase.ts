import * as admin from "firebase-admin";

// Em vez de confiar no .length, vamos tentar inicializar.
// Se der erro porque "já existe", o bloco 'catch' apanha e o código continua feliz.
try {
  admin.initializeApp();
} catch (error: any) {
  // Verificamos se o erro é o tal "duplicate-app". Se for, ignoramos.
  // Se for outro erro qualquer, mostramos na consola.
  if (!/already exists/u.test(error.message)) {
    console.error("Erro na inicialização do Firebase:", error.stack);
  }
}

// Exporta a base de dados
export const db = admin.firestore();