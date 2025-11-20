import { Request, Response, NextFunction } from "express";
import { admin } from "../config/firebase";

// Estendemos a interface do Request para incluir o utilizador descodificado
export interface AuthRequest extends Request {
  user?: admin.auth.DecodedIdToken;
}

export const isAuthenticated = async (req: AuthRequest, res: Response, next: NextFunction) => {
  const { authorization } = req.headers;

  if (!authorization || !authorization.startsWith("Bearer ")) {
    return res.status(401).json({ 
      error: "Não autorizado", 
      message: "Token de autenticação não fornecido ou inválido.",
    });
  }

  const token = authorization.split("Bearer ")[1];

  try {
    const decodedToken = await admin.auth().verifyIdToken(token);
    req.user = decodedToken; // Anexa o utilizador ao pedido para uso posterior nos controllers
    return next();
  } catch (err) {
    console.error("Erro de autenticação:", err);
    return res.status(403).json({ 
      error: "Proibido", 
      message: "Token inválido ou expirado.",
    });
  }
};