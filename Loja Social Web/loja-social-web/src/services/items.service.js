import { collection, getDocs } from "firebase/firestore";
import { db } from "./firebase";

function norm(s) {
  return String(s ?? "")
    .toLowerCase()
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

export async function getProdutosDisponiveis() {
  const snap = await getDocs(collection(db, "produtos"));
  const docs = snap.docs.map((d) => ({ id: d.id, ...d.data() }));

  // Se não quiseres filtrar, mete: return docs;
  const disponiveis = docs.filter((p) => !norm(p.estadoProduto).includes("entreg"));

  return disponiveis;
}

export function percentagensPorCategoria(produtos) {
  const counts = {};
  for (const p of produtos) {
    const cat = String(p.categoria ?? "Outros").trim() || "Outros";
    counts[cat] = (counts[cat] ?? 0) + 1;
  }

  const entries = Object.entries(counts).map(([categoria, count]) => ({ categoria, count }));
  const total = entries.reduce((a, b) => a + b.count, 0);

  const rows = entries
    .map((r) => ({
      ...r,
      percent: total ? Math.round((r.count / total) * 100) : 0,
    }))
    .sort((a, b) => b.count - a.count);

  return { total, rows };
}
