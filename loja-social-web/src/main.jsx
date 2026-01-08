// Importa o React (necessário para JSX)
import React from "react";

// Importa o ReactDOM para renderizar a aplicação no DOM
import ReactDOM from "react-dom/client";

// Importa o componente raiz da aplicação
import App from "./App.jsx";

// Importa os providers globais (contextos)
import { Providers } from "./providers.jsx";

// Importa estilos globais
import "./index.css";
import "./styles/wireframe.css";

// Importa o CSS do Leaflet (necessário para o mapa)
import "leaflet/dist/leaflet.css";

// Função utilitária para renderizar a app num elemento específico
function renderInto(el) {
  ReactDOM.createRoot(el).render(
    // StrictMode ajuda a detetar problemas em desenvolvimento
    <React.StrictMode>
      {/* Providers envolve a app com contextos globais */}
      <Providers>
        <App />
      </Providers>
    </React.StrictMode>
  );
}

// ===== Estratégia de montagem =====

// 1) Tenta primeiro montar no div específico do embed
const embedEl = document.getElementById("loja-social-react");

// 2) Caso não exista, usa o mount normal do Vite (index.html)
const rootEl = document.getElementById("root");

// Se existir o elemento de embed, dá prioridade a esse
if (embedEl) {
  // Classe opcional para permitir “scoping” de CSS apenas no embed
  embedEl.classList.add("ls-embed");

  // Renderiza a aplicação no embed
  renderInto(embedEl);
} else if (rootEl) {
  // Caso contrário, renderiza no root padrão
  renderInto(rootEl);
} else {
  // Erro caso nenhum elemento de montagem exista
  console.error("Nenhum mount encontrado (#loja-social-react ou #root).");
}
