// src/routes/index.ts

import { Router } from 'express';
import apoiadosRouter from './apoiados.routes';
// import outraColecaoRouter from './outra-colecao.routes';

const router = Router();

// Todas as rotas em 'apoiados.routes.ts' serão prefixadas com /apoiados
router.use('/apoiados', apoiadosRouter);
router.use('/funcionarios', apoiadosRouter);

// Quando tiver outra coleção (ex: 'projetos'):
// router.use('/projetos', outraColecaoRouter);

export default router;