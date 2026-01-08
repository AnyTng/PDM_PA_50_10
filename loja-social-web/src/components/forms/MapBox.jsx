// Importa o hook useEffect do React
import { useEffect } from "react";

// Importa componentes do react-leaflet
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";

// Importa o Leaflet
import L from "leaflet";

// ===== Correção dos ícones do Leaflet no Vite =====
// Sem isto, os marcadores não aparecem corretamente
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

// Remove o método interno antigo
delete L.Icon.Default.prototype._getIconUrl;

// Define manualmente os ícones do marcador
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

// ===== Coordenadas do SAS IPCA =====
const SAS_IPCA = {
  name: "Serviços de Ação Social - IPCA",
  position: [41.535627, -8.627058],
};

// ===== Componente utilitário =====
// Força o Leaflet a recalcular o tamanho do mapa
// Necessário quando o mapa está dentro de containers flex ou escondidos
function FixLeafletResize() {
  const map = useMap();

  useEffect(() => {
    // Força a correção logo após o render inicial
    const t1 = setTimeout(() => map.invalidateSize(), 50);
    const t2 = setTimeout(() => map.invalidateSize(), 250);

    // Recalcula o tamanho quando a janela muda de tamanho
    const onResize = () => map.invalidateSize();
    window.addEventListener("resize", onResize);

    // Cleanup dos timers e do event listener
    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
      window.removeEventListener("resize", onResize);
    };
  }, [map]);

  // Este componente não renderiza nada visualmente
  return null;
}

// ===== Componente principal do mapa =====
export default function MapBox() {
  return (
    // Wrapper do mapa (controlado por CSS)
    <div className="wf-mapbox">
      <MapContainer
        // Centro do mapa
        center={SAS_IPCA.position}
        // Nível de zoom inicial
        zoom={15}
        // Desativa zoom com scroll do rato
        scrollWheelZoom={false}
        // Garante que o mapa ocupa todo o espaço disponível
        style={{ height: "100%", width: "100%" }}
      >
        {/* Aplica o fix de resize do Leaflet */}
        <FixLeafletResize />

        {/* Camada base do OpenStreetMap */}
        <TileLayer
          attribution='&copy; OpenStreetMap contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {/* Marcador no local do SAS IPCA */}
        <Marker position={SAS_IPCA.position}>
          <Popup>
            <b>{SAS_IPCA.name}</b>
            <br />
            Ponto de entrega de doações
          </Popup>
        </Marker>
      </MapContainer>
    </div>
  );
}
