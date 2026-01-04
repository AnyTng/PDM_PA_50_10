export function percentByCategory(items) {
  const map = new Map();

  for (const it of items) {
    const cat = String(it.category ?? "Outros").trim() || "Outros";
    map.set(cat, (map.get(cat) ?? 0) + 1);
  }

  const rows = Array.from(map.entries()).map(([category, count]) => ({
    category,
    count,
  }));

  const total = rows.reduce((a, b) => a + b.count, 0);

  // percentagem arredondada
  const rowsWithPercent = rows
    .map((r) => ({
      ...r,
      percent: total === 0 ? 0 : Math.round((r.count / total) * 100),
    }))
    .sort((a, b) => b.count - a.count);

  return { total, rows: rowsWithPercent };
}
