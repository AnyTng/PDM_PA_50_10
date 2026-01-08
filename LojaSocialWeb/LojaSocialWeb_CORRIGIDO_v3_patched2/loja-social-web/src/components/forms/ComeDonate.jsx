// Importa o componente do mapa
import MapBox from "./MapBox.jsx";

// Componente que apresenta informações sobre onde e quando doar
export default function ComeDonate() {
  return (
    // Bloco principal da secção "Venha Doar"
    <div className="vd-block">
      {/* Título da secção */}
      <h2 className="wf-h2">Venha Doar:</h2>

      {/* Wrapper principal com layout vertical */}
      <div className="vd-wrap vd-vertical">
        {/* Área com informações textuais */}
        <div className="vd-info">
          {/* Linha: dias da semana */}
          <div className="vd-row">
            <div className="vd-label">Dias da semana</div>
            <div className="vd-value">Segunda a Quinta</div>
          </div>

          {/* Linha: horário */}
          <div className="vd-row">
            <div className="vd-label">Horário</div>
            <div className="vd-value">13:00 — 18:00</div>
          </div>

          {/* Linha: local */}
          <div className="vd-row">
            <div className="vd-label">Local</div>
            <div className="vd-value">Gabinete SAS (IPCA)</div>
          </div>

          {/* Linha: contacto */}
          <div className="vd-row">
            <div className="vd-label">Contacto</div>
            <div className="vd-value">sas@ipca.pt</div>
          </div>
        </div>

        {/* Área do mapa */}
        <div className="vd-map">
          <MapBox />
        </div>
      </div>
    </div>
  );
}
