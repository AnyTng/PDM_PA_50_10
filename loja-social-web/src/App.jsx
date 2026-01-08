// Importa os componentes principais da aplicação
import Dashboard from "./components/dashboard/Dashboard.jsx";
import DonationSection from "./components/forms/DonationSection.jsx";
import ComeDonate from "./components/forms/ComeDonate.jsx";
import NewsletterCarousel from "./components/newslatter/NewsletterCarousel.jsx";

// Importa estilos globais da aplicação
import "./styles/wireframe.css";

// Importa o CSS do Leaflet (necessário para o mapa)
import "leaflet/dist/leaflet.css";

// Componente raiz da aplicação
export default function App() {
  return (
    // Wrapper exterior (usado também no embed)
    <div className="ls-embed">
      {/* Estrutura base da página */}
      <div className="wf-page">
        <div className="wf-container">
          {/* Secção superior: dashboard */}
          <section className="wf-top">
            <Dashboard />
          </section>

          {/* Secção intermédia: mapa + formulário */}
          <section className="wf-middle">
            {/* Coluna esquerda: informações e mapa */}
            <div className="wf-left">
              <ComeDonate />
            </div>

            {/* Coluna direita: formulário de doação */}
            <div className="wf-right">
              <h2 className="wf-h2">Agende a sua Doação:</h2>
              <DonationSection />
            </div>
          </section>

          {/* Secção inferior: newsletter */}
          <section className="wf-bottom">
            <NewsletterCarousel />
          </section>
        </div>
      </div>
    </div>
  );
}
