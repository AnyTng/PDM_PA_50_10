import { Router } from "express";

// Importa as funções do NOVO controlador de funcionários
import {
  getAllFuncionarios,
  createFuncionario,
  getFuncionarioById,
  updateFuncionario,
  deleteFuncionario,
} from "../controllers/funcionarios.controller";

const router = Router();

// Rota base: /api/funcionarios
router.get("/", getAllFuncionarios);
router.post("/", createFuncionario);

// Rota com parâmetro: /api/funcionarios/:id
router.get("/:id", getFuncionarioById);
router.put("/:id", updateFuncionario);
router.delete("/:id", deleteFuncionario);

export default router;