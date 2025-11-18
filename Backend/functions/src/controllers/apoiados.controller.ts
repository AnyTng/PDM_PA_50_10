import { Request, Response } from "express";
// IMPORTANTE: Importamos a 'db' do nosso novo ficheiro de configuração
import { db } from "../config/firebase"; 

// Referência à coleção 'apoiados'
const apoiadosCollection = db.collection("apoiados");

/**
 * GET /api/apoiados
 * Lista todos os apoiados.
 */
export const getAllApoiados = async (request: Request, response: Response) => {
  try {
    const snapshot = await apoiadosCollection.get();
    const apoiados: { id: string; [key: string]: any }[] = [];
    snapshot.forEach((doc) => {
      apoiados.push({ id: doc.id, ...doc.data() });
    });
    return response.json(apoiados);
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao buscar apoiados" });
  }
};

/**
 * POST /api/apoiados
 * Cria um novo apoiado.
 */
export const createApoiado = async (request: Request, response: Response) => {
  try {
    const apoiadoData = request.body;
    const { id, nomeApoiado } = apoiadoData;

    if (!id || !nomeApoiado) {
      return response.status(400).json({ error: 'Os campos "id" e "nomeApoiado" são obrigatórios' });
    }

    const docRef = apoiadosCollection.doc(id);
    const doc = await docRef.get();

    if (doc.exists) {
      return response.status(409).json({ error: "Já existe um documento com esse ID" });
    }

    const dataToSet = { ...apoiadoData };
    delete dataToSet.id;

    await docRef.set(dataToSet);

    return response.status(201).json({ id: id, ...dataToSet });
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao criar apoiado" });
  }
};

/**
 * GET /api/apoiados/:id
 */
export const getApoiadoById = async (request: Request, response: Response) => {
  try {
    const { id } = request.params;
    const docRef = apoiadosCollection.doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return response.status(404).json({ error: "Apoiado não encontrado" });
    }
    return response.json({ id: doc.id, ...doc.data() });
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao buscar apoiado" });
  }
};

/**
 * PUT /api/apoiados/:id
 */
export const updateApoiado = async (request: Request, response: Response) => {
  try {
    const { id } = request.params;
    const dataToUpdate = request.body;

    if (dataToUpdate.id) delete dataToUpdate.id;

    if (Object.keys(dataToUpdate).length === 0) {
      return response.status(400).json({ error: "Corpo da requisição está vazio" });
    }
    
    if (!dataToUpdate.nomeApoiado) {
       return response.status(400).json({ error: "Campo 'nomeApoiado' é obrigatório" });
    }

    const docRef = apoiadosCollection.doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return response.status(404).json({ error: "Apoiado não encontrado" });
    }

    await docRef.update(dataToUpdate);

    return response.status(200).json({ id: docRef.id, ...dataToUpdate });
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao atualizar apoiado" });
  }
};

/**
 * DELETE /api/apoiados/:id
 */
export const deleteApoiado = async (request: Request, response: Response) => {
  try {
    const { id } = request.params;
    const docRef = apoiadosCollection.doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return response.status(404).json({ error: "Apoiado não encontrado" });
    }

    await docRef.delete();
    return response.status(204).send();
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao apagar apoiado" });
  }
};