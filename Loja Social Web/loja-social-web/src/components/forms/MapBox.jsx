import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";

// Corrige ícones do Leaflet no Vite (senão o marker pode não aparecer)
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

// Coordenadas (ajusta para o local exato SAS IPCA)
const SAS_IPCA = {
  name: "Serviços de Ação Social - IPCA",
  position: [41.5392, -8.6156], // Barcelos (aprox). Ajusta se quiseres.
};

export default function MapBox() {
  return (
    <div className="wf-mapbox">
      <MapContainer
        center={SAS_IPCA.position}
        zoom={15}
        scrollWheelZoom={false}
        style={{ height: "100%", width: "100%" }}
      >
        {/* API externa: OpenStreetMap */}
        <TileLayer
          attribution='&copy; OpenStreetMap contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

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
