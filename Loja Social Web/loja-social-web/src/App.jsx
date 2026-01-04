import Dashboard from "./components/dashboard/Dashboard.jsx";
import DonationSection from "./components/forms/DonationSection.jsx";
import ComeDonate from "./components/forms/ComeDonate.jsx";
import NewsletterCarousel from "./components/newslatter/NewsletterCarousel.jsx";
import "./styles/wireframe.css";
import "leaflet/dist/leaflet.css";


export default function App() {
  return (
    <div className="wf-page">
      <div className="wf-container">
      <header className="wf-header">
        <div className="wf-title">LOJA SOCIAL IPCA</div>
        <div className="wf-header-line" />
      </header>

      <section className="wf-top">
        <Dashboard />
      </section>

      <section className="wf-middle">
        <ComeDonate />

        <div className="wf-divider" />

        <div className="wf-right">
          <h2 className="wf-h2">Agende a sua Doação:</h2>
          <DonationSection />
        </div>
      </section>

      <section className="wf-bottom">
        <NewsletterCarousel />
      </section>
    </div>
    </div>
  );
}
