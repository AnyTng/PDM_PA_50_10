import { Router } from "express";
import {
  addApoiado, // Mudado de createApoiado para addApoiado
  getAllApoiados,
  getApoiadoById,
  updateApoiado,
  deleteApoiado,
} from "../controllers/apoiados.controller";

const apoiadosRoutes = Router();

apoiadosRoutes.post("/", addApoiado);
apoiadosRoutes.get("/", getAllApoiados);
apoiadosRoutes.get("/:apoiadoId", getApoiadoById);
apoiadosRoutes.put("/:apoiadoId", updateApoiado); // ou patch
apoiadosRoutes.delete("/:apoiadoId", deleteApoiado);

export { apoiadosRoutes };