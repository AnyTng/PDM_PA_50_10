import { Router } from "express";
import {
  addFuncionario, // Mudado de createFuncionario para addFuncionario
  getAllFuncionarios,
  getFuncionarioById,
  updateFuncionario,
  deleteFuncionario,
} from "../controllers/funcionarios.controller";

const funcionariosRoutes = Router();

funcionariosRoutes.post("/", addFuncionario);
funcionariosRoutes.get("/", getAllFuncionarios);
funcionariosRoutes.get("/:funcionarioId", getFuncionarioById);
funcionariosRoutes.put("/:funcionarioId", updateFuncionario);
funcionariosRoutes.delete("/:funcionarioId", deleteFuncionario);

export { funcionariosRoutes };