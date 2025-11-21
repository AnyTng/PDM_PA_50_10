import { Router } from "express";
import rateLimit from "express-rate-limit";
import { apoiadosRoutes } from "./apoiados.routes";
import { funcionariosRoutes } from "./funcionarios.routes";
import { isAuthenticated } from "../middlewares/authentication";

const router = Router();

// 2. CONFIGURAÇÃO DE SEGURANÇA (Rate Limiting)
// Define um limite de pedidos para evitar ataques
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // Janela de 15 minutos
  max: 100, // Limite de 100 pedidos por IP nessa janela
  message: {
    error: "Muitos pedidos efetuados a partir deste IP, tente novamente mais tarde.",
  },
  standardHeaders: true,
  legacyHeaders: false,
});

// 3. Aplica o limitador a TODAS as rotas
router.use(limiter);

// --- Rotas Privadas (Protegidas pelo Middleware) ---
router.use(isAuthenticated);

router.use("/apoiados", apoiadosRoutes);
router.use("/funcionarios", funcionariosRoutes);

export { router };