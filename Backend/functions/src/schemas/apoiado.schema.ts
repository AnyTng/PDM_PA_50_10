import { z } from "zod";

export const ApoiadoSchema = z.object({
  nomeApoiado: z.string().min(3, "Nome do apoiado é obrigatório"),
  emailApoiado: z.string().email("Email do apoiado inválido"),
  
  // Datas (Strings ISO "YYYY-MM-DD")
  dataNascimento: z.string().refine((d) => !isNaN(Date.parse(d)), "Data de nascimento inválida"),
  validadeConta: z.string().optional().refine((d) => !d || !isNaN(Date.parse(d)), "Data de validade inválida"),
  ultimaCesta: z.string().optional().refine((d) => !d || !isNaN(Date.parse(d)), "Data da última cesta inválida"),

  permissao: z.number().int(), 
  funcionarioID: z.string(), // Quem registou/gere
  
  ccpassaporte: z.string().min(5, "Documento de identificação inválido"),
  relacaoIPCA: z.string(), // Ex: "Aluno"
  Curso: z.string().optional(), // Nota: Letra maiúscula conforme o teu JSON
  
  necessidade: z.array(z.string()), // ["Alimentar", "Higiene"]
  
  apoioEmergenciaSocial: z.boolean(),
  bolsaEstudos: z.boolean(),
  
  docPDF: z.string().optional().default(""), 
  estadoConta: z.string().optional().default(""),
});

export type ApoiadoInput = z.infer<typeof ApoiadoSchema>;