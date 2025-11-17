// src/controllers/apoiados.controller.ts

import { Request, Response } from 'express';
import * as admin from 'firebase-admin';

// Referência à coleção 'apoiados'
const db = admin.firestore().collection('apoiados');

/**
 * GET /api/apoiados
 * Lista todos os apoiados.
 */
export const getAllApoiados = async (request: Request, response: Response) => {
  // ... (sem alterações)
  try {
    const snapshot = await db.get();
    const apoiados: { id: string; [key: string]: any }[] = [];
    snapshot.forEach(doc => {
      apoiados.push({ id: doc.id, ...doc.data() });
    });
    return response.json(apoiados);
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: 'Erro ao buscar apoiados' });
  }
};

/**
 * POST /api/apoiados
 * Cria um novo apoiado, GARANTINDO que o ID é único.
 * (MODIFICADO para aceitar todos os campos do body)
 */
export const createApoiado = async (request: Request, response: Response) => {
  try {
    // 1. Obter todos os dados do body
    const apoiadoData = request.body;
    const { id, nomeApoiado } = apoiadoData; // Usar 'nomeApoiado' da sua estrutura JSON

    // 2. Validar campos mínimos (id e nomeApoiado, conforme o seu JSON)
    if (!id || !nomeApoiado) {
      return response.status(400).json({ error: 'Os campos "id" e "nomeApoiado" são obrigatórios' });
    }

    // 3. Criar a referência ao documento com o ID fornecido
    const docRef = db.doc(id);

    // 4. Tentar ler esse documento
    const doc = await docRef.get();

    // 5. Verificar se o documento JÁ EXISTE
    if (doc.exists) {
      return response.status(409).json({ error: 'Já existe um documento com esse ID' });
    }

    // 6. Preparar os dados para salvar (remover o id, pois é a chave)
    const dataToSet = { ...apoiadoData };
    delete dataToSet.id;

    // 7. Se não existe, CRIAR o novo documento com TODOS os dados
    await docRef.set(dataToSet);

    // 8. Retornar sucesso com o objeto criado
    return response.status(201).json({ id: id, ...dataToSet });

  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: 'Erro ao criar apoiado' });
  }
};

/**
 * GET /api/apoiados/:id
 * Obtém um apoiado específico.
 */
export const getApoiadoById = async (request: Request, response: Response) => {
  // ... (sem alterações)
  try {
    const { id } = request.params;
    const docRef = db.doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return response.status(404).json({ error: 'Apoiado não encontrado' });
    }
    return response.json({ id: doc.id, ...doc.data() });
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: 'Erro ao buscar apoiado' });
  }
};

/**
 * PUT /api/apoiados/:id
 * Atualiza um apoiado.
 * (MODIFICADO para aceitar todos os campos do body)
 */
export const updateApoiado = async (request: Request, response: Response) => {
  try {
    const { id } = request.params;
    const dataToUpdate = request.body; // Obter todos os campos do body

    // Opcional: remover 'id' do corpo se estiver presente
    if (dataToUpdate.id) {
      delete dataToUpdate.id;
    }

    // Validar se o corpo não está vazio
    if (Object.keys(dataToUpdate).length === 0) {
      return response.status(400).json({ error: "Corpo da requisição está vazio" });
    }
    
    // (Pode manter a validação do nomeApoiado se for obrigatório na atualização)
    if (!dataToUpdate.nomeApoiado) {
       return response.status(400).json({ error: "Campo 'nomeApoiado' é obrigatório" });
    }

    const docRef = db.doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return response.status(404).json({ error: 'Apoiado não encontrado' });
    }

    // Atualiza o documento com todos os dados fornecidos
    await docRef.update(dataToUpdate);

    // Retorna o ID e os dados que foram enviados
    return response.status(200).json({ id: docRef.id, ...dataToUpdate });
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: 'Erro ao atualizar apoiado' });
  }
};

/**
 * DELETE /api/apoiados/:id
 * Apaga um apoiado.
 */
export const deleteApoiado = async (request: Request, response: Response) => {
  // ... (sem alterações)
  try {
    const { id } = request.params;
    const docRef = db.doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      return response.status(404).json({ error: 'Apoiado não encontrado' });
    }

    await docRef.delete();

    return response.status(204).send();
  } catch (error) {
    console.error(error);
    return response.status(500).json({ error: 'Erro ao apagar apoiado' });
  }
};