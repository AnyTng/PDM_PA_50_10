import { Request, Response } from "express";
// IMPORTANTE: Importamos a 'db' do nosso novo ficheiro de configuração
import { db } from "../config/firebase"; 

const funcionariosCollection = db.collection("funcionarios");

export const getAllFuncionarios = async (request: Request, response: Response) => {
  try {
    const snapshot = await funcionariosCollection.get();
    const funcionarios: { id: string; [key: string]: any }[] = [];
    snapshot.forEach((doc) => {
      funcionarios.push({ id: doc.id, ...doc.data() });
    });
    return response.json(funcionarios);
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao buscar funcionários" });
  }
};

export const createFuncionario = async (request: Request, response: Response) => {
  try {
    const funcionarioData = request.body;
    const { id, nome, email } = funcionarioData; 

    if (!id || !nome || !email) {
      return response.status(400).json({ error: "Os campos 'id', 'nome' e 'email' são obrigatórios" });
    }

    const docRef = funcionariosCollection.doc(id);
    const doc = await docRef.get();

    if (doc.exists) {
      return response.status(409).json({ error: "Já existe um funcionário com esse ID" });
    }

    const dataToSet = { ...funcionarioData };
    delete dataToSet.id;

    await docRef.set(dataToSet);
    return response.status(201).json({ id: id, ...dataToSet });
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao criar funcionário" });
  }
};

export const getFuncionarioById = async (request: Request, response: Response) => {
  try {
    const { id } = request.params;
    const docRef = funcionariosCollection.doc(id);
    const doc = await docRef.get();
    if (!doc.exists) {
      return response.status(404).json({ error: "Funcionário não encontrado" });
    }
    return response.json({ id: doc.id, ...doc.data() });
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao buscar funcionário" });
  }
};

export const updateFuncionario = async (request: Request, response: Response) => {
  try {
    const { id } = request.params;
    const dataToUpdate = request.body;
    if (dataToUpdate.id) delete dataToUpdate.id;
    if (Object.keys(dataToUpdate).length === 0) {
      return response.status(400).json({ error: "Corpo da requisição está vazio" });
    }
    const docRef = funcionariosCollection.doc(id);
    const doc = await docRef.get();
    if (!doc.exists) {
      return response.status(404).json({ error: "Funcionário não encontrado" });
    }
    await docRef.update(dataToUpdate);
    return response.status(200).json({ id: docRef.id, ...dataToUpdate });
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao atualizar funcionário" });
  }
};

export const deleteFuncionario = async (request: Request, response: Response) => {
  try {
    const { id } = request.params;
    const docRef = funcionariosCollection.doc(id);
    const doc = await docRef.get();
    if (!doc.exists) {
      return response.status(404).json({ error: "Funcionário não encontrado" });
    }
    await docRef.delete();
    return response.status(204).send();
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: "Erro ao apagar funcionário" });
  }
};