import { Pie } from "react-chartjs-2";
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from "chart.js";

ChartJS.register(ArcElement, Tooltip, Legend);

const palette = ["#1f77b4", "#17becf", "#ff7f0e", "#2ca02c", "#9467bd", "#7f7f7f"];

export default function DonutToBar({ rows }) {
  // rows = [{ categoria, count, percent }]
  const data = {
    labels: rows.map((r) => r.categoria),
    datasets: [
      {
        data: rows.map((r) => r.percent), // percentagens
        backgroundColor: rows.map((_, i) => palette[i % palette.length]),
        borderWidth: 0,
      },
    ],
  };

  return (
    <div style={{ width: 400, height: 380, margin: "0 auto" }}>
      <Pie
        data={data}
        options={{
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                label: (ctx) => {
                  const label = ctx.label ?? "";
                  const value = ctx.raw ?? 0;
                  return `${label}: ${value}%`;
                },
              },
            },
          },
        }}
      />
    </div>
  );
}
