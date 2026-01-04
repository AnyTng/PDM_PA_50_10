import { createContext, useContext, useMemo } from "react";
import { createRepository } from "./domain/repository.js";

const RepoCtx = createContext(null);

export function Providers({ children }) {
  const repo = useMemo(() => createRepository(), []);
  return <RepoCtx.Provider value={repo}>{children}</RepoCtx.Provider>;
}

export function useRepo() {
  const v = useContext(RepoCtx);
  if (!v) throw new Error("Repository não está disponível. Verifica Providers em main.jsx.");
  return v;
}
