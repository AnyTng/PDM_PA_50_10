// Importa funções do Firestore para criar documentos e timestamps
import { addDoc, collection, serverTimestamp } from "firebase/firestore";

// Importa Firestore e função de autenticação
import { db, ensureAuth } from "./firebase";

/**
 * Cria um pedido de envio de email na coleção "mail".
 * Compatível com a extensão Firebase "Trigger Email" (firestore-send-email).
 * A extensão observa documentos em "mail" e envia automaticamente.
 */
export async function pedirEnvioEmail({ to, subject, text }) {
  // Garante autenticação (normalmente anónima) para cumprir regras do Firestore
  await ensureAuth();

  // Cria um documento na coleção "mail" no formato esperado pela extensão
  const ref = await addDoc(collection(db, "mail"), {
    to,
    message: {
      subject,
      text,
    },
    // Timestamp do servidor para registo
    createdAt: serverTimestamp(),
  });

  // Devolve o id do pedido criado
  return ref.id;
}
