// Hooks do React
import { useMemo, useState } from "react";

// Componentes de gráficos do react-chartjs-2
import { Pie, Bar } from "react-chartjs-2";

// Importações base do Chart.js
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
} from "chart.js";

// Registo obrigatório dos módulos usados pelo Chart.js
ChartJS.register(
  ArcElement,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale
);

// Paleta de cores usada nos gráficos
const palette = ["#1f77b4", "#17becf", "#ff7f0e", "#2ca02c", "#9467bd", "#7f7f7f"];

// ===== Função utilitária =====
// Garante que o valor é uma string válida; caso contrário, usa um fallback
const clean = (v, fallback) => {
  const s = String(v ?? "").trim();
  return s ? s : fallback;
};

// Componente que alterna entre gráfico de donut e gráfico de barras
export default function DonutToBar({ produtos = [] }) {
  /**
   * produtos = lista de produtos DISPONÍVEIS (já filtrados)
   * exemplo: [{ categoria, nomeProduto, ... }]
   */

  // Estado que define a vista atual ("pie" ou "bar")
  const [view, setView] = useState("pie");

  // Categoria atualmente selecionada no donut
  const [selectedCategory, setSelectedCategory] = useState(null);

  // ===== 1) Agregação de dados para o gráfico DONUT (por categoria) =====
  const categoryRows = useMemo(() => {
    const map = new Map();

    // Conta quantos produtos existem por categoria
    for (const p of produtos) {
      const cat = clean(p.categoria, "Sem categoria");
      map.set(cat, (map.get(cat) ?? 0) + 1);
    }

    // Evita divisão por zero
    const total = produtos.length || 1;

    // Converte o Map para array e calcula percentagens
    return Array.from(map, ([categoria, count]) => ({
      categoria,
      count,
      percent: Math.round((count / total) * 100),
    }))
      // Ordena por quantidade (descendente)
      .sort((a, b) => b.count - a.count);
  }, [produtos]);

  // Labels do donut (categorias)
  const categoryLabels = useMemo(
    () => categoryRows.map((r) => r.categoria),
    [categoryRows]
  );

  // Dados do gráfico de donut
  const pieData = useMemo(
    () => ({
      labels: categoryLabels,
      datasets: [
        {
          // Usa quantidade absoluta para o tamanho das fatias
          data: categoryRows.map((r) => r.count),
          backgroundColor: categoryRows.map(
            (_, i) => palette[i % palette.length]
          ),
          borderWidth: 0,
        },
      ],
    }),
    [categoryLabels, categoryRows]
  );

  // ===== 2) Agregação de dados para o gráfico de BARRAS =====
  // Conta produtos por nome dentro da categoria selecionada
  const barRows = useMemo(() => {
    if (!selectedCategory) return [];

    const map = new Map();

    for (const p of produtos) {
      const cat = clean(p.categoria, "Sem categoria");
      if (cat !== selectedCategory) continue;

      const nome = clean(p.nomeProduto, "Sem nome");
      map.set(nome, (map.get(nome) ?? 0) + 1);
    }

    return Array.from(map, ([nomeProduto, count]) => ({
      nomeProduto,
      count,
    }))
      // Ordena por quantidade (descendente)
      .sort((a, b) => b.count - a.count);
  }, [produtos, selectedCategory]);

  // Labels do gráfico de barras (nomes dos produtos)
  const barLabels = useMemo(
    () => barRows.map((r) => r.nomeProduto),
    [barRows]
  );

  // Dados do gráfico de barras
  const barData = useMemo(
    () => ({
      labels: barLabels,
      datasets: [
        {
          label: "Quantidade",
          data: barRows.map((r) => r.count),
          backgroundColor: barRows.map(
            (_, i) => palette[i % palette.length]
          ),
          borderWidth: 0,
        },
      ],
    }),
    [barLabels, barRows]
  );

  // Estilo comum para o wrapper dos gráficos
  const commonWrapStyle = {
    width: "100%",
    maxWidth: 560,
    margin: "0 auto",
  };

  // ===== Interação =====
  // Clique numa fatia do donut:
  // seleciona a categoria e muda para o gráfico de barras
  const handlePieClick = (event, elements) => {
    if (!elements?.length) return;

    const idx = elements[0].index;
    const cat = categoryRows[idx]?.categoria;
    if (!cat) return;

    setSelectedCategory(cat);
    setView("bar");
  };

  // Total de produtos da categoria selecionada
  const selectedTotal = useMemo(() => {
    if (!selectedCategory) return 0;

    return produtos.filter(
      (p) => clean(p.categoria, "Sem categoria") === selectedCategory
    ).length;
  }, [produtos, selectedCategory]);

  // ===== Renderização =====
  return (
    <div>
      {/* Botão para voltar ao donut */}
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          gap: 10,
          marginBottom: 10,
          flexWrap: "wrap",
        }}
      >
        {selectedCategory && (
          <button
            type="button"
            className="wf-mini-btn"
            onClick={() => {
              setView("pie");
              setSelectedCategory(null);
            }}
          >
            Voltar
          </button>
        )}
      </div>

      {/* Alterna entre donut e barras */}
      {view === "pie" ? (
        <div style={{ ...commonWrapStyle, height: 360 }}>
          <Pie
            data={pieData}
            options={{
              responsive: true,
              maintainAspectRatio: false,
              onClick: handlePieClick,
              plugins: {
                legend: { position: "bottom" },
                tooltip: {
                  callbacks: {
                    // Tooltip personalizada com quantidade e percentagem
                    label: (ctx) => {
                      const label = ctx.label ?? "";
                      const value = ctx.raw ?? 0;
                      const row = categoryRows.find(
                        (r) => r.categoria === label
                      );
                      const pct = row?.percent ?? 0;
                      return `${label}: ${value} ( ${pct}% )`;
                    },
                  },
                },
              },
            }}
          />
        </div>
      ) : (
        <div style={{ ...commonWrapStyle }}>
          {selectedCategory ? (
            <>
              {/* Título da categoria selecionada */}
              <div
                style={{
                  textAlign: "center",
                  marginBottom: 8,
                  fontWeight: 700,
                }}
              >
                {selectedCategory} — {selectedTotal} itens
              </div>

              {/* Gráfico de barras */}
              <div style={{ height: 380 }}>
                <Bar
                  data={barData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                      y: { beginAtZero: true, ticks: { precision: 0 } },
                      x: {
                        ticks: {
                          maxRotation: 40,
                          minRotation: 40,
                          autoSkip: false,
                        },
                      },
                    },
                  }}
                />
              </div>
            </>
          ) : (
            // Mensagem de ajuda caso nenhuma categoria esteja selecionada
            <div style={{ textAlign: "center", padding: 12 }}>
              Clica numa categoria no donut para ver as barras por produto.
            </div>
          )}
        </div>
      )}
    </div>
  );
}
