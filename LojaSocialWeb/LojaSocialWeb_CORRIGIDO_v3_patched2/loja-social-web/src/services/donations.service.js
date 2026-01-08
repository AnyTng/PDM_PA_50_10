// Importa funções do Firestore para criar documentos e gerar timestamps no servidor
import { addDoc, collection, serverTimestamp } from "firebase/firestore";

// Importa a instância do Firestore e a função que garante autenticação
import { db, ensureAuth } from "./firebase";

// Cria um documento na coleção "doacoes" com os dados do payload
export async function criarDoacao(payload) {
  // Garante que o utilizador está autenticado (normalmente anónimo)
  // Isto é útil quando as regras do Firestore exigem request.auth != null
  await ensureAuth();

  // Adiciona um novo documento na coleção "doacoes"
  const ref = await addDoc(collection(db, "doacoes"), {
    // Espalha os campos recebidos no payload
    ...payload,

    // Guarda data/hora do servidor (mais fiável que Date.now() do cliente)
    createdAt: serverTimestamp(),
  });

  // Devolve o id do documento criado
  return ref.id;
}
