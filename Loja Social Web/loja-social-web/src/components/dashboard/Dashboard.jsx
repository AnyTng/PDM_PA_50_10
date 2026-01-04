import { useEffect, useState } from "react";
import { useRepo } from "../../providers.jsx";
import DonutToBar from "./DonutToBar.jsx";
import { percentagensPorCategoria } from "../../services/items.service.js";

export default function Dashboard() {
  const repo = useRepo();
  const [produtos, setProdutos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");
        // repo.getAvailableItems devolve {id, category, ...}
        const items = await repo.getAvailableItems();
        // transformar no formato que o teu percentagensPorCategoria espera
        const mapped = items.map((x) => ({ categoria: x.category }));
        setProdutos(mapped);
      } catch (e) {
        setErr(String(e?.message ?? e));
      } finally {
        setLoading(false);
      }
    })();
  }, [repo]);

  const { total, rows } = percentagensPorCategoria(produtos);

  return (
    <section id="dashboard" style={{ textAlign: "center" }}>
      <h2 style={{ margin: "0 0 6px" }}>Dashboard</h2>

      {loading && <p>A carregar...</p>}
      {!loading && err && <p style={{ color: "crimson" }}>Erro: {err}</p>}

      {!loading && !err && (
        <>
          {total === 0 ? (
            <p style={{ opacity: 0.8 }}>
              Sem produtos disponíveis (ou estão todos com estado <b>Entregue</b>).
            </p>
          ) : (
            <>
              <div style={{ fontSize: 13, marginBottom: 8 }}>
                Total disponíveis: <b>{total}</b>
              </div>
              <DonutToBar rows={rows} />
            </>
          )}
        </>
      )}
    </section>
  );
}
