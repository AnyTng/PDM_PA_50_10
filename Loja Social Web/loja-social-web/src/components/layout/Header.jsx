export default function Header() {
  return (
    <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
      <div>
        <h1 style={{ margin: 0 }}>Loja Social IPCA</h1>
        <p style={{ margin: "6px 0 0", opacity: 0.8 }}>
          Consulta bens disponíveis e agenda doações.
        </p>
      </div>

      <nav style={{ display: "flex", gap: 10 }}>
        <a href="#dashboard">Dashboard</a>
        <a href="#info">Informação</a>
        <a href="#doacao">Doação</a>
        <a href="#newsletter">Newsletter</a>
      </nav>
    </header>
  );
}
