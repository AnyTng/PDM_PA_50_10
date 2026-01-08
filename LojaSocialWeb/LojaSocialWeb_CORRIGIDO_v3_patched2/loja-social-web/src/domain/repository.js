// Serviços de acesso a dados
import { getProdutosDisponiveis } from "../services/items.service.js";
import { getPosts } from "../services/posts.service.js";

// Cria um repositório que centraliza o acesso aos dados
export function createRepository() {
  return {
    // ===== Dashboard =====
    // Obtém produtos disponíveis para o dashboard
    getAvailableItems: async () => {
      // Vai buscar os produtos ao serviço
      const produtos = await getProdutosDisponiveis();

      // Normaliza os dados para o formato usado na UI
      return produtos.map((p) => ({
        id: p.id,
        categoria: p.categoria ?? "Outros",

        // Campo principal do nome do produto
        // Usa fallback caso o nome não exista
        nomeProduto: p.nomeProduto ?? p.descProduto ?? "Sem nome",

        subCategoria: p.subCategoria ?? null,
        estadoProduto: p.estadoProduto ?? null,
      }));
    },

    // ===== Newsletter =====
    // Obtém posts para a newsletter
    getPosts: async (max = 10) => {
      return await getPosts(max);
    },
  };
}
