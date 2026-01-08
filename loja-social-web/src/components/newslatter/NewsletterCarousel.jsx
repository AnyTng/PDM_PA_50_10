// Hooks do React
import { useEffect, useState } from "react";

// Hook personalizado para aceder ao repositório
import { useRepo } from "../../providers.jsx";

// Componente que mostra um carrossel simples de posts da newsletter
export default function NewsletterCarousel() {
  // Obtém o repositório (fonte de dados)
  const repo = useRepo();

  // Lista de posts carregados
  const [posts, setPosts] = useState([]);

  // Índice inicial do carrossel
  const [idx, setIdx] = useState(0);

  // Carrega os posts ao montar o componente
  useEffect(() => {
    repo
      .getPosts(6)       // pede no máximo 6 posts
      .then(setPosts)    // guarda no estado
      .catch(() => setPosts([])); // fallback em caso de erro
  }, [repo]);

  // Posts atualmente visíveis (3 de cada vez)
  const visible = posts.slice(idx, idx + 3);

  // Vai para o item anterior
  function prev() {
    setIdx((v) => Math.max(0, v - 1));
  }

  // Vai para o próximo item
  function next() {
    // Garante que não ultrapassa o limite
    setIdx((v) => Math.min(Math.max(0, posts.length - 3), v + 1));
  }

  return (
    // Secção principal da newsletter
    <section
      id="newsletter"
      style={{ padding: 16, border: "1px solid #e5e7eb", borderRadius: 16 }}
    >
      {/* Cabeçalho com título e botões */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          gap: 12,
        }}
      >
        <h2 style={{ marginTop: 0 }}>Newsletter</h2>

        {/* Botões de navegação */}
        <div style={{ display: "flex", gap: 8 }}>
          <button onClick={prev} disabled={idx === 0}>
            ◀
          </button>
          <button onClick={next} disabled={idx >= posts.length - 3}>
            ▶
          </button>
        </div>
      </div>

      {/* Conteúdo */}
      {posts.length === 0 ? (
        // Mensagem quando não existem posts
        <p>
          Sem posts. Cria documentos na coleção <b>posts</b> (ex.: title,
          excerpt).
        </p>
      ) : (
        // Grelha com os posts visíveis
        <div
          style={{
            marginTop: 12,
            display: "grid",
            gridTemplateColumns: "repeat(3, 1fr)",
            gap: 10,
          }}
        >
          {visible.map((p) => (
            // Cartão individual de post
            <div
              key={p.id}
              style={{
                border: "1px solid #e5e7eb",
                borderRadius: 12,
                padding: 12,
              }}
            >
              <b>{p.title ?? "Sem título"}</b>
              <p style={{ margin: "8px 0 0", opacity: 0.85 }}>
                {p.excerpt ?? ""}
              </p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
