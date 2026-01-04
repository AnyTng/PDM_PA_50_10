import { collection, getDocs, orderBy, query, limit } from "firebase/firestore";
import { db } from "./firebase";

export async function getPosts(max = 10) {
  const q = query(collection(db, "posts"), orderBy("createdAt", "desc"), limit(max));
  const snap = await getDocs(q);

  return snap.docs.map((d) => ({ id: d.id, ...d.data() }));
}
