import { Router } from "express";
import apoiadosRoutes from "./apoiados.routes";
import funcionariosRoutes from "./funcionarios.routes"; 
const router = Router();

// Rota para Apoiados -> usa o ficheiro apoiados.routes.ts
router.use("/apoiados", apoiadosRoutes);

// Rota para Funcionários -> usa o ficheiro funcionarios.routes.ts
router.use("/funcionarios", funcionariosRoutes);

export default router;