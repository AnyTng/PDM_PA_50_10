// Hooks do React para contexto e memoização
import { createContext, useContext, useMemo } from "react";

// Função que cria o repositório de acesso aos dados
import { createRepository } from "./domain/repository.js";

// Cria o contexto que vai guardar o repositório
const RepoCtx = createContext(null);

// Componente Provider que disponibiliza o repositório a toda a aplicação
export function Providers({ children }) {
  // Cria o repositório apenas uma vez
  // useMemo garante que a instância não é recriada a cada render
  const repo = useMemo(() => createRepository(), []);

  // Disponibiliza o repositório através do Context
  return <RepoCtx.Provider value={repo}>{children}</RepoCtx.Provider>;
}

// Hook personalizado para consumir o repositório
export function useRepo() {
  // Lê o valor do contexto
  const v = useContext(RepoCtx);

  // Se não existir repositório, é porque o Provider não foi usado
  if (!v) {
    throw new Error(
      "Repository não está disponível. Verifica Providers em main.jsx."
    );
  }

  // Devolve o repositório para uso nos componentes
  return v;
}
