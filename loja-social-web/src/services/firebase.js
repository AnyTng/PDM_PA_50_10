// Inicialização do Firebase
import { initializeApp } from "firebase/app";

// Firestore (base de dados)
import { getFirestore } from "firebase/firestore";

// Auth (autenticação)
import { getAuth, onAuthStateChanged, signInAnonymously } from "firebase/auth";

// Configuração lida das variáveis de ambiente (Vite)
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FB_API_KEY,
  authDomain: import.meta.env.VITE_FB_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FB_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FB_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FB_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FB_APP_ID,
};

// Cria a app Firebase
const app = initializeApp(firebaseConfig);

// Exporta a instância do Firestore para usar noutros ficheiros
export const db = getFirestore(app);

// ===== Auth anónimo =====
// Ajuda a evitar erros de permissões quando as regras exigem request.auth.
// Nota: é necessário ativar "Anonymous" em Firebase Authentication.
export const auth = getAuth(app);

// Guarda a Promise para não repetir tentativas de autenticação
let _ensureAuthPromise = null;

// Função que garante que existe um utilizador autenticado
export function ensureAuth() {
  // Se já estiver em curso (ou concluída), reutiliza a mesma Promise
  if (_ensureAuthPromise) return _ensureAuthPromise;

  // Cria a Promise que resolve quando houver utilizador autenticado
  _ensureAuthPromise = new Promise((resolve) => {
    // Observa mudanças no estado de autenticação
    const unsub = onAuthStateChanged(auth, async (user) => {
      // Se já existe utilizador, resolve e pára de ouvir
      if (user) {
        unsub();
        resolve(user);
        return;
      }

      // Se não existe utilizador, tenta login anónimo
      try {
        await signInAnonymously(auth);
      } catch {
        // Se o login anónimo não estiver ativo, não bloqueia a aplicação
        // (mas poderá haver erros de permissões nas operações do Firestore)
      }
    });
  });

  return _ensureAuthPromise;
}

// Faz disparar a autenticação cedo (logo ao carregar o módulo)
ensureAuth();
