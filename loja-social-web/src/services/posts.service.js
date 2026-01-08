// Importa helpers do Firestore para queries
import { collection, getDocs, orderBy, query, limit } from "firebase/firestore";

// Importa a instância do Firestore
import { db } from "./firebase";

// Vai buscar posts à coleção "posts" ordenados por createdAt desc
export async function getPosts(max = 10) {
  // Cria a query: posts mais recentes primeiro, limitado a max
  const q = query(
    collection(db, "posts"),
    orderBy("createdAt", "desc"),
    limit(max)
  );

  // Executa a query
  const snap = await getDocs(q);

  // Converte docs para objetos JS e inclui o id do documento
  return snap.docs.map((d) => ({ id: d.id, ...d.data() }));
}
