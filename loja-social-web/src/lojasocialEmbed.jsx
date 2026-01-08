// Importa o React (necessário para JSX)
import React from "react";

// Importa o ReactDOM para renderizar a app no DOM
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

// Elemento HTML onde a aplicação React será montada
const mountEl = document.getElementById("loja-social-react");

// Verifica se o elemento existe no HTML
if (!mountEl) {
  // Erro claro caso o container não exista
  console.error('Não existe <div id="loja-social-react"></div> no HTML embed.');
} else {
  // Cria a raiz React e renderiza a aplicação
  ReactDOM.createRoot(mountEl).render(
    // StrictMode ajuda a detetar problemas em desenvolvimento
    <React.StrictMode>
      {/* Providers envolve a app com contextos globais */}
      <Providers>
        <App />
      </Providers>
    </React.StrictMode>
  );
}
