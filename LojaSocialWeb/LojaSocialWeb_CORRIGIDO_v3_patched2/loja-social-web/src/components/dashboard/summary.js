// Calcula a quantidade e percentagem de itens por categoria
export function percentByCategory(items) {
  // Map para contar quantos itens existem por categoria
  const map = new Map();

  // Percorre todos os itens recebidos
  for (const it of items) {
    // Normaliza a categoria:
    // - usa "Outros" se não existir
    // - remove espaços
    // - garante que não fica vazia
    const cat = String(it.category ?? "Outros").trim() || "Outros";

    // Incrementa o contador da categoria
    map.set(cat, (map.get(cat) ?? 0) + 1);
  }

  // Converte o Map num array de objetos { category, count }
  const rows = Array.from(map.entries()).map(([category, count]) => ({
    category,
    count,
  }));

  // Calcula o total de itens somando todos os counts
  const total = rows.reduce((acc, r) => acc + r.count, 0);

  // Calcula a percentagem de cada categoria (arredondada)
  // e ordena por quantidade (descendente)
  const rowsWithPercent = rows
    .map((r) => ({
      ...r,
      percent: total === 0 ? 0 : Math.round((r.count / total) * 100),
    }))
    .sort((a, b) => b.count - a.count);

  // Devolve o total e as linhas com percentagem
  return { total, rows: rowsWithPercent };
}
