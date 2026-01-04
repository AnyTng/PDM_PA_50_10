import { addDoc, collection, serverTimestamp } from "firebase/firestore";
import { db } from "./firebase";

export async function criarDoacao(payload) {
  const ref = await addDoc(collection(db, "doacoes"), {
    ...payload,
    createdAt: serverTimestamp(),
  });
  return ref.id;
}
