import { getProdutosDisponiveis } from "../services/items.service.js";
import { getPosts } from "../services/posts.service.js";

export function createRepository() {
  return {
    // dashboard
    getAvailableItems: async () => {
      const produtos = await getProdutosDisponiveis();
      return produtos.map((p) => ({
        id: p.id,
        category: p.categoria ?? "Outros",
        estadoProduto: p.estadoProduto ?? null,
      }));
    },

    // newsletter
    getPosts: async (max = 10) => {
      return await getPosts(max);
    },
  };
}
