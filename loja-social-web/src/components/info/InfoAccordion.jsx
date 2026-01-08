// Importa o hook useState do React
import { useState } from "react";

// Conteúdo do accordion (títulos e textos)
const items = [
  {
    title: "Tipos de apoios disponibilizados",
    body: "Bens alimentares, higiene e limpeza (e outros consoante campanhas).",
  },
  {
    title: "A quem se destina?",
    body: "Comunidade académica com carências socioeconómicas comprovadas.",
  },
  {
    title: "Como recorrer?",
    body: "Contacto com os SAS/IPCA e processo de apoio conforme regulamento.",
  },
  {
    title: "Posso contribuir, doando?",
    body: "Sim. Agenda a doação e entrega nos horários definidos.",
  },
  {
    title: "Regulamento",
    body: "Consulta o regulamento e condições de acesso/entrega.",
  },
];

// Componente de accordion informativo
export default function InfoAccordion() {
  // Guarda o índice do item atualmente aberto
  // -1 significa que nenhum item está aberto
  const [open, setOpen] = useState(0);

  return (
    // Secção principal da informação
    <section
      id="info"
      style={{
        padding: 16,
        border: "1px solid #e5e7eb",
        borderRadius: 16,
      }}
    >
      {/* Título da secção */}
      <h2 style={{ marginTop: 0 }}>Informação</h2>

      {/* Lista de itens do accordion */}
      <div style={{ display: "grid", gap: 8 }}>
        {items.map((it, idx) => (
          // Container de cada item
          <div
            key={it.title}
            style={{
              border: "1px solid #e5e7eb",
              borderRadius: 12,
            }}
          >
            {/* Botão que abre/fecha o item */}
            <button
              onClick={() => setOpen(open === idx ? -1 : idx)}
              style={{
                width: "100%",
                textAlign: "left",
                padding: 12,
                background: "transparent",
                border: 0,
                cursor: "pointer",
              }}
            >
              <b>{it.title}</b>
            </button>

            {/* Conteúdo visível apenas quando o item está aberto */}
            {open === idx && (
              <div style={{ padding: "0 12px 12px", opacity: 0.9 }}>
                {it.body}
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
