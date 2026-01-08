// Importa funções do Firestore para ler documentos
import { collection, getDocs } from "firebase/firestore";

// Importa a instância do Firestore
import { db } from "./firebase";

// Normaliza texto para facilitar comparações:
// - lowercase
// - trim
// - remove acentos
function norm(s) {
  return String(s ?? "")
    .toLowerCase()
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "");
}

// Tenta converter vários formatos possíveis para Date:
// - Firestore Timestamp (toDate)
// - objeto serializado com {seconds}
// - Date nativo
// - string (inclui dd/mm/yyyy ou ISO)
function toDateMaybe(v) {
  if (!v) return null;

  // Firestore Timestamp
  if (typeof v?.toDate === "function") return v.toDate();

  // Timestamp serializado
  if (typeof v?.seconds === "number") return new Date(v.seconds * 1000);

  // Date já é Date
  if (v instanceof Date) return v;

  // Strings
  if (typeof v === "string") {
    const s = v.trim();

    // Formato dd/mm/yyyy
    const m = s.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
    if (m) return new Date(Number(m[3]), Number(m[2]) - 1, Number(m[1]));

    // Tenta Date() genérico (ISO, etc.)
    const d = new Date(s);
    return Number.isNaN(d.getTime()) ? null : d;
  }

  // Qualquer outro tipo não é suportado
  return null;
}

// Verifica se o produto está dentro da validade
function isWithinValidity(p) {
  // Nos docs, o campo costuma chamar-se "validade"
  const exp = toDateMaybe(p.validade);

  // Se não houver data de validade, considera válido
  if (!exp) return true;

  // "Hoje" sem horas para comparar só por dia
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // Data de expiração também normalizada para dia
  const expiry = new Date(exp);
  expiry.setHours(0, 0, 0, 0);

  // Válido se expiração for hoje ou no futuro
  return expiry >= today;
}

// Decide se um produto deve ser considerado "doado"
function isDonated(p) {
  // Em muitos casos "doado" é true/false ou string
  // Aqui tratamos qualquer valor "verdadeiro" como doado
  const v = p.doado;
  if (v === true) return true;
  if (typeof v === "string") return v.trim().length > 0;
  return Boolean(v);
}

// Lê produtos no Firestore e devolve apenas os que estão disponíveis
export async function getProdutosDisponiveis() {
  // Busca todos os docs da coleção "produtos"
  const snap = await getDocs(collection(db, "produtos"));

  // Converte para objetos JS e adiciona o id do documento
  const docs = snap.docs.map((d) => ({ id: d.id, ...d.data() }));

  // Regras de disponibilidade:
  // - NÃO doado
  // - NÃO entregue (estadoProduto não contém "entreg")
  // - dentro do prazo de validade
  const disponiveis = docs.filter((p) => {
    const estado = norm(p.estadoProduto);
    const naoEntregue = !estado.includes("entreg");
    const naoDoado = !isDonated(p);
    const dentroValidade = isWithinValidity(p);
    return naoEntregue && naoDoado && dentroValidade;
  });

  return disponiveis;
}

// Calcula percentagens de produtos por categoria (para gráficos/resumos)
export function percentagensPorCategoria(produtos) {
  // Objeto para contagens por categoria
  const counts = {};

  // Conta quantos produtos existem por categoria
  for (const p of produtos) {
    const cat = String(p.categoria ?? "Outros").trim() || "Outros";
    counts[cat] = (counts[cat] ?? 0) + 1;
  }

  // Converte {cat: count} para array [{categoria, count}]
  const entries = Object.entries(counts).map(([categoria, count]) => ({
    categoria,
    count,
  }));

  // Total de produtos (soma de todos os counts)
  const total = entries.reduce((a, b) => a + b.count, 0);

  // Adiciona percentagem e ordena por quantidade desc
  const rows = entries
    .map((r) => ({
      ...r,
      percent: total ? Math.round((r.count / total) * 100) : 0,
    }))
    .sort((a, b) => b.count - a.count);

  // Devolve total + linhas
  return { total, rows };
}
