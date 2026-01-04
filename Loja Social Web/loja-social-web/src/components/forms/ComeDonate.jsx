import MapBox from "./MapBox.jsx";

export default function ComeDonate() {
  return (
    <div className="vd-block">
      {/* Título fora do grid interno (alinha com "Agende a sua Doação") */}
      <h2 className="wf-h2">Venha Doar:</h2>

      <div className="vd-wrap">
        <div className="vd-left">
          <div className="vd-info">
            <div className="vd-row">
              <div className="vd-label">Dias da semana</div>
              <div className="vd-value">Segunda a Quinta</div>
            </div>

            <div className="vd-row">
              <div className="vd-label">Horário</div>
              <div className="vd-value">13:00 — 18:00</div>
            </div>

            <div className="vd-row">
              <div className="vd-label">Local</div>
              <div className="vd-value">Gabinete SAS (IPCA)</div>
            </div>

            <div className="vd-row">
              <div className="vd-label">Contacto</div>
              <div className="vd-value">sas@ipca.pt</div>
            </div>
          </div>
        </div>

        <div className="vd-right">
          <div className="vd-map">
            <MapBox />
          </div>
        </div>
      </div>
    </div>
  );
}
