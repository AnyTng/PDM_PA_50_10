// Hooks do React para gerir estado e efeitos
import { useEffect, useState } from "react";

// Biblioteca para sanitizar HTML e evitar XSS
import DOMPurify from "dompurify";

// CSS específico para este embed
import "./lojaSocialEmbed.css";

// Extrai do HTML original apenas a parte principal (título + conteúdo)
function extractMain(htmlText) {
  // Converte a string HTML num documento DOM manipulável
  const doc = new DOMParser().parseFromString(htmlText, "text/html");

  // ===== 1) Extrair o título =====
  // Procura um <h1> com a classe "page-title"
  const titleEl = doc.querySelector("h1.page-title");
  // Se existir, usa o texto; senão, usa fallback
  const title = titleEl ? titleEl.textContent.trim() : "Loja Social";

  // ===== 2) Extrair o conteúdo principal =====
  // Procura o container típico do WordPress com a classe "entry-content"
  const entry = doc.querySelector(".entry-content");
  if (!entry) {
    // Se não encontrar, devolve mensagem de erro simples
    return { title, contentHtml: "<p>Conteúdo não encontrado.</p>" };
  }

  // ===== 3) Remover elementos potencialmente perigosos =====
  // Remove scripts/estilos/iframes/links para aumentar segurança
  entry
    .querySelectorAll("script, style, link, iframe")
    .forEach((n) => n.remove());

  // ===== 4) Substituir accordion do WP por <details> =====
  // O accordion do WordPress normalmente depende de JS externo.
  // Aqui convertemos para um formato nativo do HTML (<details>/<summary>).
  const accordion = entry.querySelector(".accordion");
  if (accordion) {
    // Obtém todos os títulos do accordion
    const titles = Array.from(accordion.querySelectorAll(".accordion-title"));

    titles.forEach((t) => {
      // O conteúdo costuma estar associado via aria-controls
      const controlsId = t.getAttribute("aria-controls");

      // Procura o elemento do conteúdo correspondente ao id
      const content = controlsId
        ? accordion.querySelector(`#${CSS.escape(controlsId)}`)
        : null;

      // Cria o <details> que vai substituir o item do accordion
      const details = doc.createElement("details");
      details.className = "ls-details";

      // Cria o <summary> (a "barra" clicável)
      const summary = doc.createElement("summary");
      summary.className = "ls-summary";
      summary.textContent = t.textContent.trim();

      // Cria um container para o conteúdo
      const body = doc.createElement("div");
      body.className = "ls-details-body";

      // Copia o HTML do conteúdo original, se existir
      if (content) body.innerHTML = content.innerHTML;

      // Monta a estrutura final <details>
      details.appendChild(summary);
      details.appendChild(body);

      // Insere o <details> antes do título original
      t.parentNode.insertBefore(details, t);

      // Remove o conteúdo original e o título original
      if (content) content.remove();
      t.remove();
    });

    // Remove conteúdos restantes do accordion, caso tenham ficado (limpeza)
    accordion.querySelectorAll(".accordion-content").forEach((n) => n.remove());
  }

  // ===== 5) Sanitizar o HTML final =====
  // Garante que o HTML injetado é seguro
  const cleanHtml = DOMPurify.sanitize(entry.innerHTML, {
    USE_PROFILES: { html: true },
  });

  // Devolve título + HTML limpo para renderizar
  return { title, contentHtml: cleanHtml };
}

// Componente que faz fetch de um HTML local e embebe o conteúdo no React
export default function LojaSocialFromHtml() {
  // Estado único para guardar título, conteúdo e erro
  const [state, setState] = useState({
    title: "",
    contentHtml: "",
    error: "",
  });

  useEffect(() => {
    // Flag para evitar setState depois do componente desmontar
    let alive = true;

    // Faz fetch do ficheiro HTML (ex: colocado em /public)
    fetch("/loja-social-sas.html")
      .then((r) => {
        // Se a resposta não for ok, lança erro
        if (!r.ok) throw new Error("Não consegui ler /loja-social.html");
        // Converte para texto (HTML)
        return r.text();
      })
      .then((html) => {
        // Se o componente já desmontou, não faz nada
        if (!alive) return;

        // Extrai título e conteúdo do HTML
        const { title, contentHtml } = extractMain(html);

        // Atualiza estado com o resultado
        setState({ title, contentHtml, error: "" });
      })
      .catch((e) => {
        // Se o componente já desmontou, não faz nada
        if (!alive) return;

        // Em caso de erro, guarda mensagem e mantém um título fallback
        setState({ title: "Loja Social", contentHtml: "", error: e.message });
      });

    // Cleanup: marca como desmontado
    return () => {
      alive = false;
    };
  }, []);

  return (
    // Secção principal do embed
    <section className="ls-embed">
      {/* Cabeçalho com o título */}
      <header className="ls-embed__header">
        <h1 className="ls-embed__title">{state.title || "Loja Social"}</h1>
      </header>

      {/* Se existir erro, mostra a mensagem; caso contrário renderiza HTML */}
      {state.error ? (
        <p className="ls-embed__error">{state.error}</p>
      ) : (
        // Injeta HTML sanitizado no DOM (perigoso sem sanitização)
        <div
          className="ls-embed__content"
          dangerouslySetInnerHTML={{ __html: state.contentHtml }}
        />
      )}
    </section>
  );
}
