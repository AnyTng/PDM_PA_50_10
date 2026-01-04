import { useState } from "react";
import { criarDoacao } from "../../services/donations.service.js";

export default function DonationForm() {
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [telefone, setTelefone] = useState("");
  const [categoria, setCategoria] = useState("Alimentar");
  const [data, setData] = useState("");
  const [hora, setHora] = useState("");
  const [mensagem, setMensagem] = useState("");

  const [ok, setOk] = useState("");
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setOk("");
    setErr("");

    if (!nome.trim() || !email.trim() || !telefone.trim()) {
      setErr("Preenche Nome, Email e Telefone.");
      return;
    }

    try {
      setLoading(true);
      await criarDoacao({
        nome: nome.trim(),
        email: email.trim(),
        telefone: telefone.trim(),
        categoria,
        data: data || null,
        hora: hora || null,
        mensagem: mensagem.trim() || null,
        estado: "Novo",
      });
      setOk("Enviado!");
      setNome("");
      setEmail("");
      setTelefone("");
      setCategoria("Alimentar");
      setData("");
      setHora("");
      setMensagem("");
    } catch (e2) {
      setErr(String(e2?.message ?? e2));
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="don-form">
      

      <input placeholder="Nome" value={nome} onChange={(e) => setNome(e.target.value)} />
      <input placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
      <input placeholder="Telefone" value={telefone} onChange={(e) => setTelefone(e.target.value)} />

      <select value={categoria} onChange={(e) => setCategoria(e.target.value)}>
        <option value="Alimentar">Alimentar</option>
        <option value="Higiene">Higiene</option>
        <option value="Limpeza">Limpeza</option>
        <option value="Outros">Outros</option>
      </select>

      <div className="don-row">
        <input type="date" value={data} onChange={(e) => setData(e.target.value)} />
        <input type="time" value={hora} onChange={(e) => setHora(e.target.value)} />
      </div>

      <textarea placeholder="Mensagem" rows={4} value={mensagem} onChange={(e) => setMensagem(e.target.value)} />

      {err && <div className="don-msg don-err">{err}</div>}
      {ok && <div className="don-msg don-ok">{ok}</div>}

      <button disabled={loading} type="submit">
        {loading ? "A enviar..." : "Enviar"}
      </button>
    </form>
  );
}
