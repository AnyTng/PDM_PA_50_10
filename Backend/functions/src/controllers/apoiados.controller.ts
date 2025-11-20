import { Response } from "express";
import { db } from "../config/firebase";
import { AuthRequest } from "../middlewares/authentication";
import { ApoiadoSchema } from "../schemas/apoiado.schema";

type Controller = (req: AuthRequest, res: Response) => Promise<Response | void>;

export const addApoiado: Controller = async (req, res) => {
  try {
    const validatedData = ApoiadoSchema.parse(req.body);

    const dadosProntosParaDB = {
      ...validatedData,
      dataNascimento: new Date(validatedData.dataNascimento),
      validadeConta: validatedData.validadeConta ? new Date(validatedData.validadeConta) : null,
      ultimaCesta: validatedData.ultimaCesta ? new Date(validatedData.ultimaCesta) : null,
      criadoPor: req.user?.uid,
      criadoEm: new Date(),
    };

    const docRef = await db.collection("apoiados").add(dadosProntosParaDB);

    return res.status(201).json({
      id: docRef.id,
      message: "Apoiado criado com sucesso",
      data: validatedData,
    });
  } catch (error: any) {
    if (error.name === "ZodError") {
      return res.status(400).json({ error: "Dados inválidos", details: error.errors });
    }
    return res.status(500).json({ error: error.message });
  }
};

export const getAllApoiados: Controller = async (req, res) => {
  try {
    const snapshot = await db.collection("apoiados").get();
    const apoiados = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    return res.status(200).json(apoiados);
  } catch (error: any) {
    return res.status(500).json({ error: error.message });
  }
};

// --- ADICIONAR ESTA FUNÇÃO ---
export const getApoiadoById: Controller = async (req, res) => {
  const { apoiadoId } = req.params;
  try {
    const doc = await db.collection("apoiados").doc(apoiadoId).get();
    if (!doc.exists) {
      return res.status(404).json({ error: "Apoiado não encontrado" });
    }
    return res.status(200).json({ id: doc.id, ...doc.data() });
  } catch (error: any) {
    return res.status(500).json({ error: error.message });
  }
};
// -----------------------------

export const updateApoiado: Controller = async (req, res) => {
  const { apoiadoId } = req.params;
  
  try {
    const docRef = db.collection("apoiados").doc(apoiadoId);
    const doc = await docRef.get();

    // VERIFICAÇÃO 404
    if (!doc.exists) {
      return res.status(404).json({ error: "Apoiado não encontrado" });
    }

    const validatedData = ApoiadoSchema.partial().parse(req.body);
    
    const dadosParaAtualizar: any = { ...validatedData };
    if (validatedData.dataNascimento) dadosParaAtualizar.dataNascimento = new Date(validatedData.dataNascimento);
    if (validatedData.validadeConta) dadosParaAtualizar.validadeConta = new Date(validatedData.validadeConta);
    if (validatedData.ultimaCesta) dadosParaAtualizar.ultimaCesta = new Date(validatedData.ultimaCesta);

    await docRef.update(dadosParaAtualizar);

    return res.status(200).json({ message: "Apoiado atualizado com sucesso" });
  } catch (error: any) {
    if (error.name === "ZodError") return res.status(400).json({ error: "Dados inválidos", details: error.errors });
    return res.status(500).json({ error: error.message });
  }
};

export const deleteApoiado: Controller = async (req, res) => {
  const { apoiadoId } = req.params;
  try {
    const docRef = db.collection("apoiados").doc(apoiadoId);
    const doc = await docRef.get();

    // VERIFICAÇÃO 404
    if (!doc.exists) {
      return res.status(404).json({ error: "Apoiado não encontrado" });
    }

    await docRef.delete();
    return res.status(200).json({ message: "Apoiado removido com sucesso" });
  } catch (error: any) {
    return res.status(500).json({ error: error.message });
  }
};