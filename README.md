<img width=100% src="https://capsule-render.vercel.app/api?type=waving&color=00bfbf&height=120&section=header"/>

# Loja Social – IPCA (Projeto 50+10)

Projeto aplicado do 3º ano da **Licenciatura em Engenharia de Sistemas Informáticos (IPCA/EST)**, desenvolvido no âmbito das UC **Projeto Aplicado (PA)** e **Programação de Dispositivos Móveis (PDM)**.

A solução inclui:
- **Aplicação Android** (para colaboradores dos SAS e estudantes apoiados);
- **Website informativo** (para a comunidade académica, com divulgação e apoio ao agendamento de doações);
- **Backend Firebase** (Firestore/Auth/Storage/FCM + Cloud Functions).

---

## Motivação e contexto

A **Loja Social do IPCA** apoia estudantes através da distribuição de bens essenciais. O processo existente era maioritariamente **manual**, o que aumenta o risco de erros, dificulta o controlo de stock e torna mais complexo o agendamento/seguimento de entregas.

Este projeto pretende **digitalizar e modernizar** esses procedimentos, permitindo:
- gestão estruturada de **beneficiários** e **inventário**;
- **agendamento** e registo de entregas/cabazes;
- maior **transparência** e comunicação com a comunidade (campanhas e necessidades de stock).

---

## Funcionalidades principais

### Aplicação móvel (Android)
- Gestão de **beneficiários** (CRUD: registar/editar/remover).
- Gestão de **inventário/stock** e estados de produto (incl. alertas de validade).
- **Agendamento** de entregas e visualização em calendário (com lembretes).
- Preparação de **cabazes** (seleção de bens a partir do stock).
- Registo de **entregas** (Entregue / Não Entregue) e baixa automática de stock.
- **Histórico** por estudante (apoios mensais).
- Registo de **campanhas/doações** (origem, totais, movimento de entradas).
- **Notificações** (estado do pedido/conta, pedidos, lembretes, etc.).
- Perfis e permissões: **Colaborador** vs **Estudante**.

### Website informativo
- Dashboard de necessidades/stock (por categoria).
- Informação “**Como doar**” (locais, horários, regras).
- Secção de **notícias/campanhas**.
- Formulário/fluxo de **agendamento de doação** (atualmente abre email do utilizador).

> Nota: o website usa Firebase (Firestore/Auth) e Leaflet (mapa).

### Backend (Firebase)
- **Firestore** como BD principal.
- **Firebase Auth** (contas de utilizador).
- **Firebase Storage** (documentos/anexos, quando aplicável).
- **Firebase Cloud Messaging** (push notifications).
- **Cloud Functions** (TypeScript) para notificações e tarefas agendadas (ex.: lembretes e alertas).

---

## Tecnologias e ferramentas

### Mobile
- **Android Studio**
- **Kotlin** + **Jetpack Compose**
- **Hilt** (injeção de dependências)
- **Firebase** (Auth, Firestore, Storage, Functions, Messaging)

### Web
- **React** + **Vite**
- **Firebase Web SDK**
- **Chart.js** / `react-chartjs-2` (gráficos)
- **Leaflet** / `react-leaflet` (mapa)
- `dompurify` (higienização de HTML)

### Backend
- **Firebase Cloud Functions (v2)** em **TypeScript**
- **Node.js 22**
- Firebase Admin SDK (Firestore/Auth/FCM)

---

## Estrutura do repositório

```text
/
├─ AndroidApp/               # App Android + Cloud Functions (Firebase)
│  ├─ app/                   # Código da app (Compose)
│  ├─ functions/             # Cloud Functions (TypeScript)
│  └─ firebase.json
├─ loja-social-web/          # Website (React + Vite)
│  ├─ src/
│  ├─ firestore.rules
│  └─ firebase.json          # Hosting + rules
└─ Documentos do Projeto/    # Relatórios, diagramas, poster, etc.
```

---

## Pré-requisitos

- **Git**
- **Node.js** (recomendado **>= 20**; para Functions o projeto está configurado para **Node 22**)
- **Firebase CLI**:
  ```bash
  npm i -g firebase-tools
  ```
- **Android Studio** + SDK Android (JDK 11/17 conforme configuração do Android Studio)

---

## Configuração (Firebase)

O projeto está preparado para o Firebase project:
- `app-loja-social-ipca` (definido em `.firebaserc`)

Se vais usar **um Firebase próprio**:
1. Cria um projeto no Firebase.
2. Ativa:
   - **Firestore**
   - **Authentication** (pelo menos Email/Password; e para o website recomenda-se também **Anonymous**)
   - **Storage**
   - **Cloud Messaging**
3. Atualiza os ficheiros de configuração:
   - Android: substitui o `AndroidApp/app/google-services.json`
   - Web: cria o `.env` com as variáveis Vite (ver abaixo)
4. (Opcional) Ajusta as **regras do Firestore** conforme a tua política de acesso.

### Variáveis de ambiente (website)
Criar ficheiro `loja-social-web/.env`:

```bash
VITE_FB_API_KEY=...
VITE_FB_AUTH_DOMAIN=...
VITE_FB_PROJECT_ID=...
VITE_FB_STORAGE_BUCKET=...
VITE_FB_MESSAGING_SENDER_ID=...
VITE_FB_APP_ID=...
```

---

## Instalação e utilização

### 1) Website (React + Vite)
```bash
cd loja-social-web
npm install
npm run dev
```

Depois abre: `http://localhost:5173`

**Build de produção**
```bash
npm run build
npm run preview
```

**Deploy (Firebase Hosting)**
```bash
firebase login
firebase use <o_teu_project_id>
firebase deploy --only hosting
```

> ⚠️ Atenção às regras `firestore.rules`: nesta versão permitem apenas `create` em `doacoes` e `mail` e bloqueiam o resto.  
> Se quiseres que o dashboard/newsletter leia `produtos` e `posts` pelo frontend, tens de ajustar as regras (ex.: permitir `read` nessas coleções).

---

### 2) Aplicação Android
1. Abrir a pasta `AndroidApp/` no **Android Studio**.
2. Sincronizar o Gradle.
3. Executar num emulador ou dispositivo.

**Notas úteis**
- `local.properties` é gerado automaticamente pelo Android Studio (caminho do SDK).
- Para notificações em Android 13+ pode ser necessário aceitar a permissão de notificações.

---

### 3) Cloud Functions (Firebase)
```bash
cd AndroidApp/functions
npm install
npm run build
firebase deploy --only functions
```

---

## Orientação

- **Doutora Patrícia Isabel Sousa Trindade Silva Leite**
- **Engenheiro Lourenço Miguel Araújo Gomes**

---

## Autores

- **Fábio Alexandre Gomes Fernandes** — a22996  
- **Fábio Rafael Gomes Costa** — a22997  
- **Luís Pedro Pereira Freitas** — a23008  
- **Lino Emanuel Oliveira Azevedo** — a23015  
- **Gonçalo Tierri Martinho Gonçalves** — a23020  

---

## Licença

Este projeto é disponibilizado sob licença **MIT** (ver ficheiro `LICENSE`).

---

## Documentação

Relatórios e artefactos do projeto podem ser encontrados em `Documentos do Projeto/` (diagramas, relatórios por entrega, poster, etc.).
<img width=100% src="https://capsule-render.vercel.app/api?type=waving&color=00bfbf&height=120&section=footer"/>

