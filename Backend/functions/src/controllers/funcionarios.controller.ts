import { Response } from "express";
import { db } from "../config/firebase";
import { AuthRequest } from "../middlewares/authentication";
import { FuncionarioSchema } from "../schemas/funcionario.schema";

type Controller = (req: AuthRequest, res: Response) => Promise<Response | void>;

export const addFuncionario: Controller = async (req, res) => {
  try {
    const validatedData = FuncionarioSchema.parse(req.body);

    const userCheck = await db.collection("funcionarios").where("email", "==", validatedData.email).get();
    if (!userCheck.empty) {
        return res.status(400).json({ error: "Funcionário com este email já existe." });
    }

    const docRef = await db.collection("funcionarios").add({
        ...validatedData,
        criadoEm: new Date().toISOString(),
    });

    return res.status(201).json({
      id: docRef.id,
      message: "Funcionário adicionado com sucesso",
      data: validatedData,
    });
  } catch (error: any) {
    if (error.name === "ZodError") {
      return res.status(400).json({ error: "Dados inválidos", details: error.errors });
    }
    return res.status(500).json({ error: error.message });
  }
};

export const getAllFuncionarios: Controller = async (req, res) => {
  try {
    const snapshot = await db.collection("funcionarios").get();
    const funcionarios = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    return res.status(200).json(funcionarios);
  } catch (error: any) {
    return res.status(500).json({ error: error.message });
  }
};

// --- ADICIONAR ESTA FUNÇÃO ---
export const getFuncionarioById: Controller = async (req, res) => {
  const { funcionarioId } = req.params;
  try {
    const doc = await db.collection("funcionarios").doc(funcionarioId).get();
    if (!doc.exists) {
      return res.status(404).json({ error: "Funcionário não encontrado" });
    }
    return res.status(200).json({ id: doc.id, ...doc.data() });
  } catch (error: any) {
    return res.status(500).json({ error: error.message });
  }
};
// -----------------------------

export const updateFuncionario: Controller = async (req, res) => {
    const { funcionarioId } = req.params;
    try {
      const docRef = db.collection("funcionarios").doc(funcionarioId);
      const doc = await docRef.get();

      // VERIFICAÇÃO 404
      if (!doc.exists) {
        return res.status(404).json({ error: "Funcionário não encontrado" });
      }

      const validatedData = FuncionarioSchema.partial().parse(req.body);
      await docRef.update(validatedData);
      
      return res.status(200).json({ message: "Funcionário atualizado" });
    } catch (error: any) {
      if (error.name === "ZodError") return res.status(400).json({ error: "Dados inválidos", details: error.errors });
      return res.status(500).json({ error: error.message });
    }
  };

export const deleteFuncionario: Controller = async (req, res) => {
    const { funcionarioId } = req.params;
    try {
      const docRef = db.collection("funcionarios").doc(funcionarioId);
      const doc = await docRef.get();

      // VERIFICAÇÃO 404
      if (!doc.exists) {
        return res.status(404).json({ error: "Funcionário não encontrado" });
      }

      await docRef.delete();
      return res.status(200).json({ message: "Funcionário removido" });
    } catch (error: any) {
      return res.status(500).json({ error: error.message });
    }
  };