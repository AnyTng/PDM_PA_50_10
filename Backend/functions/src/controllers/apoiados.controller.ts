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
  try {
    const snapshot = await db.get();
    const apoiados: { id: string; [key: string]: any }[] = [];
    snapshot.forEach(doc => {
      apoiados.push({ id: doc.id, ...doc.data() });
    });
    // Adicionado 'return'
    return response.json(apoiados);
  } catch (error) {
    console.error(error);
    // Adicionado 'return'
    return response.status(500).json({ error: 'Erro ao buscar apoiados' });
  }
};

/**
 * POST /api/apoiados
 * Cria um novo apoiado, GARANTINDO que o ID é único.
 */
export const createApoiado = async (request: Request, response: Response) => {
  try {
    const { id, nome } = request.body;

    if (!id || !nome) {
      return response.status(400).json({ error: 'Os campos "id" e "nome" são obrigatórios' });
    }

    // 1. Criar a referência ao documento com o ID fornecido
    const docRef = db.doc(id);

    // 2. Tentar ler esse documento
    const doc = await docRef.get();

    // 3. Verificar se o documento JÁ EXISTE
    if (doc.exists) {
      // 4. Se existe, retornar um erro 409 (Conflict)
      return response.status(409).json({ error: 'Já existe um documento com esse ID' });
    }

    // 5. Se não existe, CRIAR o novo documento
    await docRef.set({ nome });

    // 6. Retornar sucesso
    return response.status(201).json({ id: id, nome: nome });

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
  try {
    const { id } = request.params;
    const docRef = db.doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      // Adicionado 'return'
      return response.status(404).json({ error: 'Apoiado não encontrado' });
    }
    // Adicionado 'return'
    return response.json({ id: doc.id, ...doc.data() });
  } catch (error) {
    console.error(error);
    // Adicionado 'return'
    return response.status(500).json({ error: 'Erro ao buscar apoiado' });
  }
};

/**
 * PUT /api/apoiados/:id
 * Atualiza um apoiado.
 */
export const updateApoiado = async (request: Request, response: Response) => {
  try {
    const { id } = request.params;
    const { nome } = request.body; 

    if (!nome) {
      // Adicionado 'return'
      return response.status(400).json({ error: "Campo 'nome' é obrigatório" });
    }

    const docRef = db.doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      // Adicionado 'return'
      return response.status(404).json({ error: 'Apoiado não encontrado' });
    }

    await docRef.update({ nome });

    // Adicionado 'return'
    return response.status(200).json({ id: docRef.id, nome });
  } catch (error) {
    console.error(error);
    // Adicionado 'return'
    return response.status(500).json({ error: 'Erro ao atualizar apoiado' });
  }
};

/**
 * DELETE /api/apoiados/:id
 * Apaga um apoiado.
 */
export const deleteApoiado = async (request: Request, response: Response) => {
  try {
    const { id } = request.params;
    const docRef = db.doc(id);
    const doc = await docRef.get();

    if (!doc.exists) {
      // Adicionado 'return'
      return response.status(404).json({ error: 'Apoiado não encontrado' });
    }

    await docRef.delete();

    // Adicionado 'return'
    return response.status(204).send();
  } catch (error) {
    console.error(error);
    // Adicionado 'return'
    return response.status(500).json({ error: 'Erro ao apagar apoiado' });
  }
};