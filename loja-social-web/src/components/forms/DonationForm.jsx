// Importa o hook useState do React
import { useState } from "react";

// Formulário para pedido de doação
export default function DonationForm() {
  // Estados dos campos do formulário
  const [nome, setNome] = useState("");
  const [telefone, setTelefone] = useState("");
  const [categoria, setCategoria] = useState("Alimentar");
  const [data, setData] = useState("");
  const [hora, setHora] = useState("");
  const [mensagem, setMensagem] = useState("");

  // Estados de feedback ao utilizador
  const [ok, setOk] = useState("");
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(false);

  // Função chamada ao submeter o formulário
  function onSubmit(e) {
    // Impede o comportamento padrão do formulário
    e.preventDefault();

    // Limpa mensagens anteriores
    setOk("");
    setErr("");

    // Validação mínima: nome e telefone são obrigatórios
    if (!nome.trim() || !telefone.trim()) {
      setErr("Preenche Nome e Telefone.");
      return;
    }

    // Ativa estado de carregamento
    setLoading(true);

    // Dados do email
    const to = "sasocial.ipca@gmail.com";
    const subject = "Novo pedido de doação - Loja Social";

    // Corpo do email
    const body =
`Novo pedido de doação

Nome: ${nome}
Telefone: ${telefone}
Categoria: ${categoria}
Data: ${data || "-"}
Hora: ${hora || "-"}

Mensagem:
${mensagem?.trim() ? mensagem.trim() : "-"}

(Enviado via Loja Social)`;

    // Cria o link mailto com subject e body codificados
    const mailto = `mailto:${to}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;

    // Redireciona para o cliente de email do utilizador
    window.location.href = mailto;

    // Mensagem de sucesso
    setOk("A abrir o email para envio…");

    // Simula fim do carregamento
    window.setTimeout(() => setLoading(false), 600);
  }

  // Renderização do formulário
  return (
    <form onSubmit={onSubmit} className="don-form">
      {/* Campo: nome */}
      <div className="don-field">
        <div className="don-label">NOME</div>
        <input
          placeholder="Nome"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
        />
      </div>

      {/* Campo: telefone */}
      <div className="don-field">
        <div className="don-label">TELEFONE</div>
        <input
          placeholder="Telefone"
          value={telefone}
          onChange={(e) => setTelefone(e.target.value)}
        />
      </div>

      {/* Campo: categoria */}
      <div className="don-field">
        <div className="don-label">CATEGORIA</div>
        <select
          value={categoria}
          onChange={(e) => setCategoria(e.target.value)}
        >
          <option value="Alimentar">Alimentar</option>
          <option value="Higiene">Higiene</option>
          <option value="Limpeza">Limpeza</option>
          <option value="Outros">Outros</option>
        </select>
      </div>

      {/* Linha com data e hora */}
      <div className="don-row">
        <div className="don-field">
          <div className="don-label">DATA</div>
          <input
            type="date"
            value={data}
            onChange={(e) => setData(e.target.value)}
          />
        </div>

        <div className="don-field">
          <div className="don-label">HORA</div>
          <input
            type="time"
            value={hora}
            onChange={(e) => setHora(e.target.value)}
          />
        </div>
      </div>

      {/* Campo: mensagem */}
      <div className="don-field">
        <div className="don-label">MENSAGEM</div>
        <textarea
          placeholder="Mensagem"
          rows={4}
          value={mensagem}
          onChange={(e) => setMensagem(e.target.value)}
        />
      </div>

      {/* Mensagens de erro e sucesso */}
      {err && <div className="don-msg don-err">{err}</div>}
      {ok && <div className="don-msg don-ok">{ok}</div>}

      {/* Botão de submissão */}
      <button disabled={loading} type="submit">
        {loading ? "A abrir..." : "Enviar"}
      </button>
    </form>
  );
}
