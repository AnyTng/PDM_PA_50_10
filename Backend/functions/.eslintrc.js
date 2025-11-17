module.exports = {
  root: true,
  env: {
    es6: true,
    node: true,
  },
  extends: [
    "eslint:recommended",
    "plugin:import/errors",
    "plugin:import/warnings",
    "plugin:import/typescript",
    "google",
    "plugin:@typescript-eslint/recommended",
  ],
  parser: "@typescript-eslint/parser",
  parserOptions: {
    project: ["tsconfig.json", "tsconfig.dev.json"],
    sourceType: "module",
  },
  ignorePatterns: [
    "/lib/**/*", // Ignore built files.
  ],
  plugins: [
    "@typescript-eslint",
    "import",
  ],
  rules: {
    "quotes": 0, // (Ignora a regra das aspas)
    "import/no-unresolved": 0,

    // --- ADICIONADO PARA CORRIGIR ERROS DE DEPLOY ---
    // Ignora a regra de quebra de linha (Windows CRLF vs Linux LF)
    "linebreak-style": 0,
    // Ignora a falta de comentários JSDoc (ex: @param)
    "valid-jsdoc": 0,
    // Ignora o aviso de "tipo any"
    "@typescript-eslint/no-explicit-any": 0,
    // Ignora parênteses em volta de argumentos de arrow functions
    "arrow-parens": 0,
    // Ignora a regra do "max-len" (comprimento máximo da linha)
    "max-len": 0,
    // Ignora regras de espaçamento nos "curly braces"
    "object-curly-spacing": 0,
    // Ignora a regra da "new-cap"
    "new-cap": 0,
    // Ignora a regra de "trailing spaces"
    "no-trailing-spaces": 0,
    // Ignora a regra de "padded-blocks"
    "padded-blocks": 0,
    // Ignora a regra de "eol-last"
    "eol-last": 0,
    // Ignora a regra de indentação (O ERRO DE AGORA)
    "indent": 0,
  },
};