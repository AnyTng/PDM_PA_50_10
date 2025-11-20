import { z } from "zod";

export const FuncionarioSchema = z.object({
  nome: z.string().min(3, "O nome é muito curto"),
  email: z.string().email("Email inválido"),
  contacto: z.string().min(9, "Contacto deve ter pelo menos 9 dígitos"),
  funcao: z.string().min(2, "Função é obrigatória"), // Ex: "Coordenadora"
  nif: z.string().length(9, "NIF deve ter exatamente 9 dígitos"),
  morada: z.string().min(5, "Morada muito curta"),
  codPostal: z.string().regex(/^\d{4}-\d{3}$/, "Código Postal inválido (ex: 4700-000)"),
  // Espera string "1990-03-15"
  dataNascimento: z.string().refine((data) => !isNaN(Date.parse(data)), {
    message: "Data de nascimento deve ser válida (AAAA-MM-DD)",
  }),
});

export type FuncionarioInput = z.infer<typeof FuncionarioSchema>;