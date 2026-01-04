import { useEffect, useState } from "react";
import { useRepo } from "../../providers.jsx";

export default function NewsletterCarousel() {
  const repo = useRepo();
  const [posts, setPosts] = useState([]);
  const [idx, setIdx] = useState(0);

  useEffect(() => {
    repo.getPosts(6).then(setPosts).catch(() => setPosts([]));
  }, [repo]);

  const visible = posts.slice(idx, idx + 3);

  function prev() {
    setIdx((v) => Math.max(0, v - 1));
  }
  function next() {
    setIdx((v) => Math.min(Math.max(0, posts.length - 3), v + 1));
  }

  return (
    <section id="newsletter" style={{ padding: 16, border: "1px solid #e5e7eb", borderRadius: 16 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
        <h2 style={{ marginTop: 0 }}>Newsletter</h2>
        <div style={{ display: "flex", gap: 8 }}>
          <button onClick={prev} disabled={idx === 0}>◀</button>
          <button onClick={next} disabled={idx >= posts.length - 3}>▶</button>
        </div>
      </div>

      {posts.length === 0 ? (
        <p>Sem posts. Cria documentos na coleção <b>posts</b> (ex.: title, excerpt).</p>
      ) : (
        <div style={{ marginTop: 12, display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 10 }}>
          {visible.map((p) => (
            <div key={p.id} style={{ border: "1px solid #e5e7eb", borderRadius: 12, padding: 12 }}>
              <b>{p.title ?? "Sem título"}</b>
              <p style={{ margin: "8px 0 0", opacity: 0.85 }}>{p.excerpt ?? ""}</p>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
