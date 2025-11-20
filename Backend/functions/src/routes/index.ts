import { Router } from "express";
import { apoiadosRoutes } from "./apoiados.routes";
import { funcionariosRoutes } from "./funcionarios.routes";
import { isAuthenticated } from "../middlewares/authentication";

const router = Router();

// --- Rotas Públicas (se houver) ---
// router.get('/status', (req, res) => res.send('OK'));

// --- Rotas Privadas (Protegidas pelo Middleware) ---
// Tudo o que estiver abaixo desta linha exige Token Bearer
router.use(isAuthenticated);

router.use("/apoiados", apoiadosRoutes);
router.use("/funcionarios", funcionariosRoutes);

export { router };