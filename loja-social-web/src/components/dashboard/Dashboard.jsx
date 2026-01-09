// Importa os hooks useEffect e useState do React
import { useEffect, useState } from "react";

// Importa o hook personalizado useRepo para acesso ao repositório de dados
import { useRepo } from "../../providers.jsx";

// Importa o componente de visualização (gráfico)
import DonutToBar from "./DonutToBar.jsx";

// Componente principal do Dashboard
export default function Dashboard() {
  // Obtém a instância do repositório
  const repo = useRepo();

  // Estado que guarda a lista de produtos disponíveis
  const [produtos, setProdutos] = useState([]);

  // Estado para controlar o carregamento dos dados
  const [loading, setLoading] = useState(true);

  // Estado para armazenar mensagens de erro
  const [err, setErr] = useState("");

  // useEffect executado quando o componente monta
  // ou quando a referência do repo muda
  useEffect(() => {
    // Função assíncrona autoexecutada
    (async () => {
      try {
        // Ativa o estado de loading
        setLoading(true);

        // Limpa erros anteriores
        setErr("");

        // Obtém os produtos disponíveis do repositório
        // Cada item tem: { id, categoria, nomeProduto, ... }
        const items = await repo.getAvailableItems();

        // Atualiza o estado com os produtos recebidos
        setProdutos(items);
      } catch (e) {
        // Em caso de erro, guarda a mensagem no estado
        setErr(String(e?.message ?? e));
      } finally {
        // Desativa o loading independentemente do resultado
        setLoading(false);
      }
    })();
  }, [repo]);

  // Calcula o total de produtos disponíveis
  const total = produtos.length;

  // Renderização do componente
  return (
    <section id="dashboard" style={{ textAlign: "center" }}>
      {/* Título do dashboard */}
      <h2 style={{ margin: "0 0 6px" }}>Dashboard</h2>

      {/* Mostra mensagem enquanto os dados estão a carregar */}
      {loading && <p>A carregar...</p>}

      {/* Mostra erro se existir e não estiver a carregar */}
      {!loading && err && <p style={{ color: "crimson" }}>Erro: {err}</p>}

      {/* Conteúdo principal quando não há loading nem erro */}
      {!loading && !err && (
        <>
          {/* Caso não existam produtos disponíveis */}
          {total === 0 ? (
            <p style={{ opacity: 0.8 }}>
              Sem produtos disponíveis (ou estão todos com estado <b>Entregue</b>).
            </p>
          ) : (
            <>
              {/* Mostra o total de produtos */}
              <div style={{ fontSize: 13, marginBottom: 8 }}>
                Total disponíveis: <b>{total}</b>
              </div>

              {/* Componente gráfico que recebe os produtos */}
              <DonutToBar produtos={produtos} />
            </>
          )}
        </>
      )}
    </section>
  );
}
