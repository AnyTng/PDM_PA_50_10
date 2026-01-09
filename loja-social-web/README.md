# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) (or [oxc](https://oxc.rs) when used in [rolldown-vite](https://vite.dev/guide/rolldown)) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## React Compiler

The React Compiler is enabled on this template. See [this documentation](https://react.dev/learn/react-compiler) for more information.

Note: This will impact Vite dev & build performances.

---

## Loja Social – notas do projeto

### Envio de email (sem abrir Outlook/Gmail)

O formulário cria um documento em `doacoes` e, em seguida, cria um documento na coleção `mail`.

Para o email ser enviado automaticamente, instala no Firebase a extensão **Trigger Email (firestore-send-email)**
e configura o provedor (ex.: SendGrid) e o remetente.

### Firestore rules

Este repositório inclui um `firestore.rules` com permissões mínimas para:
- criar documentos em `doacoes`
- criar documentos em `mail` (para a extensão)

As regras assumem que existe autenticação (o frontend tenta login anónimo automaticamente).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.
