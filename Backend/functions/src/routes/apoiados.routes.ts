import { Router } from 'express';

// Importa todas as funções do controlador
import {
  getAllApoiados,
  createApoiado,
  getApoiadoById,
  updateApoiado,
  deleteApoiado,
} from '../controllers/apoiados.controller';

const router = Router();

// Rota base /
router.get('/', getAllApoiados);
router.post('/', createApoiado);

// Rota com parâmetro /:id
router.get('/:id', getApoiadoById);
router.put('/:id', updateApoiado);
router.delete('/:id', deleteApoiado);

export default router;