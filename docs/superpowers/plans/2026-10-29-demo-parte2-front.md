# Demo tela dividida — Parte 2: front Next.js — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar em `web/` o site da demo: sistema de tarefas à esquerda, linha do tempo das chamadas reais da API à direita, cadastro descartável, bloqueio diário e botão "Ver somente API".

**Architecture:** Next.js 16 (App Router) exportado como site estático (`output: 'export'`), uma página client-side. Toda chamada passa por um cliente único (`src/lib/api.ts`) que, além de anexar o token, **registra cada requisição/resposta real** numa store pequena (`src/lib/timeline.ts`) lida pelo painel direito. Dados do servidor ficam no TanStack Query com refetch automático desligado, para a linha do tempo mostrar só o que o visitante provocou. Em dev, o `next.config.ts` (por fase) faz proxy de `/api/*` para a API local; em produção o Caddy faz isso (Parte 3) — o front sempre chama `/api`.

**Tech Stack:** Node 24, npm, Next.js 16, React 19, TypeScript estrito, Tailwind v4, TanStack Query v5, Vitest + Testing Library + jsdom.

**Spec:** `docs/superpowers/specs/2026-10-28-demo-tela-dividida-design.md` (seções 2, 4, 6, 7, 10). A Parte 1 (API) está pronta na branch `feat/demo-api`; contrato da API em `taskapi.md` §6–7.

## Global Constraints

- Tudo dentro de `web/` (pacote próprio, `package.json` próprio). Nada muda no backend nesta parte.
- TypeScript `strict`; sem `any`; props com `type`. Código e nomes em inglês; **textos da interface em português**.
- Base da API fixa: `/api` (mesma origem). Sem variável de ambiente, sem CORS.
- Dependências novas permitidas (o Thiago está aprendendo Next.js: explicar o porquê de cada uma ao instalar):
  - scaffold do `create-next-app` (next, react, react-dom, tailwindcss v4, eslint) — base do projeto;
  - `@tanstack/react-query` — cache e invalidação das listas após criar/editar/excluir;
  - dev: `vitest`, `@vitejs/plugin-react`, `vite-tsconfig-paths`, `jsdom`, `@testing-library/react`, `@testing-library/dom`, `@testing-library/user-event`, `@testing-library/jest-dom` — setup oficial de testes do Next com Vitest.
  - **Nenhuma outra.** Sem React Hook Form/Zod: a validação que importa mostrar é a da API (o 400 real aparece no campo e na linha do tempo); validar no cliente esconderia isso.
- Visual aprovado (spec §6): sistema claro, acento índigo `#4f46e5`; painel da API escuro `#0f172a`, fonte monoespaçada, selos por método — POST verde, GET azul, PUT laranja, DELETE vermelho.
- Textos exatos: bloqueio diário = mensagem da API `"Limite de usuários que testaram o sistema hoje atingido"`; aviso da conta = `"Conta temporária de demonstração, apagada em 24 h"`.
- Datas da API vêm em **UTC sem offset** (ex.: `2026-10-29T02:10:25`): tratar como UTC e exibir em `America/Sao_Paulo`.
- Token JWT em `sessionStorage` (sobrevive a recarregar a aba, some ao fechar). Tradeoff de XSS aceito: conta descartável, token de 1 h, nenhum HTML de usuário renderizado como HTML.
- Comandos de verificação, rodados em `web/`: `npm test` (Vitest), `npm run lint`, `npx tsc --noEmit`, `npm run build` (gera `web/out/`).
- Commits em português, Conventional Commits, com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, **cada um só depois do ok do Thiago na task**.

## Review Focus

- **Linha do tempo mente se o cliente "enfeitar" o registro:** o corpo mostrado tem que ser o enviado/recebido de verdade, inclusive em erro (400 com `fields`, 404, 409, 422, 429). → `api.test.ts`: `recordsRealErrorBody`.
- **Token aparecendo inteiro na tela:** o header `Authorization` aparece abreviado (`Bearer eyJhbGciOi…`) — a linha do tempo é pública e pode ir para print/vídeo. → `api.test.ts`: `abbreviatesAuthorizationHeader`.
- **Os dois 429 confundidos:** só o 429 de `POST /users` com a mensagem exata trava a demo; o 429 por IP mostra aviso de espera e não trava. → `api.test.ts`: `classifiesDailyLimit` e `Onboarding.test.tsx`: `showsLockScreenOnDailyLimit`.
- **Token expirado / conta apagada no meio do uso:** qualquer 401 com token volta para o cadastro com aviso, limpando token e cache. → `App.test.tsx`: `unauthorizedReturnsToOnboarding`.
- **Visitante digita durante a animação:** focar/editar um campo interrompe o preenchimento automático e o que ele digitou é o que vai na requisição. → `useTypewriter.test.ts`: `stopsWhenUserEdits`.

---

### Task 1: Projeto Next.js em `web/` com export estático, proxy de dev e testes

**Files:**
- Create: `web/` via `npx create-next-app@latest web --ts --tailwind --eslint --app --src-dir --import-alias "@/*" --use-npm` (aceitar os demais padrões; responder "não" ao React Compiler para manter o aprendizado explícito)
- Modify: `web/next.config.ts` (por fase)
- Create: `web/vitest.config.mts`, `web/src/test/setup.ts`
- Modify: `web/package.json` (script `"test": "vitest run"`)
- Modify: `.gitignore` da raiz (`web/node_modules/`, `web/.next/`, `web/out/` — se o `.gitignore` gerado em `web/` já cobrir, não duplicar)
- Test: `web/src/app/page.test.tsx`

**Interfaces:**
- Produces: `npm run dev` em `web/` serve em `http://localhost:3000` e `/api/*` vai para `http://localhost:8081/*`; `npm run build` gera `web/out/index.html`.
- `next.config.ts` exporta função `(phase) => NextConfig`: em `PHASE_DEVELOPMENT_SERVER`, `rewrites()` com `{ source: '/api/:path*', destination: 'http://localhost:8081/:path*' }`; nas outras fases, `{ output: 'export' }`. Motivo: rewrites não são suportados com `output: 'export'` e dão erro até no `next dev`.

- [ ] **Step 1: Gerar o projeto e explicar ao Thiago** o que cada pasta/arquivo gerado faz (App Router, `layout.tsx` × `page.tsx`, Tailwind v4 em CSS).
- [ ] **Step 2: Instalar as dependências de teste** (lista das Global Constraints) e criar `vitest.config.mts` com `plugins: [tsconfigPaths(), react()]`, `test: { environment: 'jsdom', setupFiles: ['./src/test/setup.ts'] }`; `setup.ts` importa `@testing-library/jest-dom/vitest`.
- [ ] **Step 3: Teste que falha** `page.test.tsx` → `rendersDemoTitle`: renderiza `<Page />` e espera `screen.getByRole('heading', { name: 'Task API' })`.
- [ ] **Step 4: Rodar e ver falhar** — `npm test` → FAIL (a página padrão não tem esse heading).
- [ ] **Step 5: Trocar a página padrão** por um placeholder com `<h1>Task API</h1>`; configurar `next.config.ts` por fase.
- [ ] **Step 6: Verificar**
  - `npm test` → PASS.
  - `npm run lint` e `npx tsc --noEmit` → sem erros.
  - `npm run build` → cria `web/out/index.html`.
  - Com a API rodando (`./mvnw spring-boot:run` na raiz) e `npm run dev`: `curl -s localhost:3000/api/v3/api-docs | head -c 80` → JSON do OpenAPI.
- [ ] **Step 7: Commit (após ok do Thiago)** — `feat: cria o front da demo em Next.js com export estático`

---

### Task 2: Cliente da API e store da linha do tempo

**Files:**
- Create: `web/src/lib/timeline.ts`, `web/src/lib/api.ts`, `web/src/lib/session.ts`, `web/src/lib/dates.ts`
- Create: `web/src/lib/types.ts` (tipos das respostas da API: `UserResponse`, `LoginResponse`, `CategoryResponse`, `TagResponse`, `TaskResponse`, `TaskStatus`, `TaskPriority`, `ErrorResponse`, `ValidationErrorResponse` — espelho dos records Java em `src/main/java/.../dto/`)
- Test: `web/src/lib/api.test.ts`, `web/src/lib/timeline.test.ts`, `web/src/lib/dates.test.ts`

**Interfaces:**
- Produces:
  - `type TimelineEntry = { id: number; method: 'GET' | 'POST' | 'PUT' | 'DELETE'; path: string; requestHeaders: Record<string, string>; requestBody: unknown; status: number | null; responseBody: unknown; durationMs: number | null; startedAt: number }` — `status: null` enquanto a requisição está em voo.
  - `timeline.ts`: `addEntry(e: Omit<TimelineEntry, 'id'>): number`, `updateEntry(id: number, patch: Partial<TimelineEntry>): void`, `clearTimeline(): void`, `useTimeline(): TimelineEntry[]` (via `useSyncExternalStore`; ordem: mais recente primeiro). Sem lib de estado: a store é um array em módulo + lista de listeners.
  - `api.ts`: `API_BASE = '/api'`; `class ApiError extends Error { status: number; body: ErrorResponse | ValidationErrorResponse | null; fields: Record<string, string> }`; `api<T>(method, path, body?): Promise<T>` — anexa `Authorization: Bearer <token>` se houver, `Accept`/`Content-Type: application/json`, registra entrada antes do `fetch` e atualiza com status/corpo/duração depois (também em erro e em falha de rede: `status: 0`). No registro, `Authorization` vira `Bearer ` + 10 primeiros caracteres + `…`; o path registrado inclui `/api`.
  - `api.ts`: `configureApi({ onUnauthorized: () => void })` — chamado em todo 401 **com** token; `isDailyLimit(err: unknown): boolean` — `true` só para `ApiError` 429 cujo `body.message` é exatamente a mensagem do teto diário.
  - `session.ts`: `getToken(): string | null`, `setToken(token: string): void`, `clearToken(): void` (chave `sessionStorage` `task-api-demo-token`, tudo em try/catch).
  - `dates.ts`: `formatApiDate(value: string | null): string` — interpreta como UTC (acrescenta `Z`), formata `dd/MM/yyyy HH:mm` em `America/Sao_Paulo` com `Intl.DateTimeFormat('pt-BR')`; `null` → `"—"`. `toApiDateTime(local: string): string` — valor de `<input type="datetime-local">` (horário de SP) → string UTC sem offset para enviar.

- [ ] **Step 1: Testes que falham** (com `vi.stubGlobal('fetch', …)`, sem MSW):
  - `timeline.test.ts` → `newestEntryComesFirst`, `updateEntryChangesOnlyThatEntry`.
  - `api.test.ts` → `sendsBearerTokenWhenLoggedIn`; `abbreviatesAuthorizationHeader` (registro não contém o token inteiro, contém `Bearer ` + 10 chars + `…`); `recordsRealErrorBody` (resposta 400 `{status:400, error:'Bad Request', fields:{title:'O título é obrigatório'}}` → `responseBody` igual, `ApiError.fields.title` igual); `callsOnUnauthorizedOn401WithToken` e não chama sem token; `classifiesDailyLimit` (429 com a mensagem exata → `true`; 429 "Muitas requisições…" → `false`); `recordsNetworkFailureAsStatusZero`; `returnsUndefinedOn204`.
  - `dates.test.ts` → `formatsUtcAsSaoPaulo` (`'2026-10-29T02:10:25'` → `'28/10/2026 23:10'`), `dashForNull`, `convertsLocalInputToUtc` (`'2026-11-30T18:00'` → `'2026-11-30T21:00:00'`).
- [ ] **Step 2: Rodar e ver falhar** — `npm test` → FAIL (módulos não existem).
- [ ] **Step 3: Implementar** conforme as Interfaces.
- [ ] **Step 4: Verificar** — `npm test`, `npm run lint`, `npx tsc --noEmit` → tudo passa.
- [ ] **Step 5: Commit (após ok)** — `feat: cliente da API que registra cada chamada na linha do tempo`

---

### Task 3: Casca da tela — topo, divisão, abas no celular e painel da linha do tempo

**Files:**
- Create: `web/src/components/TopBar.tsx`, `web/src/components/SplitLayout.tsx`, `web/src/components/timeline/TimelinePanel.tsx`, `web/src/components/timeline/TimelineRow.tsx`
- Create: `web/src/app/providers.tsx` (`QueryClientProvider`; `'use client'`)
- Modify: `web/src/app/layout.tsx` (título "Task API — demo", `lang="pt-BR"`, providers), `web/src/app/page.tsx`
- Test: `web/src/components/timeline/TimelinePanel.test.tsx`, `web/src/components/SplitLayout.test.tsx`

**Interfaces:**
- Consumes: `useTimeline()`, `TimelineEntry` (Task 2).
- Produces:
  - `QueryClient` com `defaultOptions.queries = { staleTime: Infinity, refetchOnWindowFocus: false, refetchOnReconnect: false, retry: false }` e `mutations.retry: false` — **nenhuma chamada automática**: cada linha da linha do tempo corresponde a uma ação do visitante.
  - `TopBar`: nome "Task API", botão-link **"Ver somente API"** → `/api/swagger-ui/index.html` (mesma aba), link do GitHub `https://github.com/ThiagoHeckler/Task_API`.
  - `SplitLayout({ system, api }: { system: ReactNode; api: ReactNode })`: ≥ 768 px, duas colunas lado a lado; < 768 px, abas "Sistema" / "API" (`role="tablist"`), com um contador de chamadas novas na aba API enquanto ela não está visível.
  - `TimelinePanel({ waiting }: { waiting: boolean })`: lista do `useTimeline()`; `waiting` mostra no topo a linha "aguardando você salvar…"; estado vazio: "As chamadas da API aparecem aqui".
  - `TimelineRow`: selo do método, path, status (cor por faixa: 2xx verde, 4xx amarelo, 5xx/0 vermelho; `null` = "…"), duração; clicar expande request (headers + corpo) e resposta (corpo) em JSON indentado; a mais recente começa expandida.
- [ ] **Step 1: Testes que falham**:
  - `TimelinePanel.test.tsx` → `showsEmptyState`, `showsEntriesNewestFirst`, `expandsRowToShowRealBodies`, `showsWaitingLine`.
  - `SplitLayout.test.tsx` → `rendersTabsOnNarrowScreen` (mock de `matchMedia` < 768 px) e `switchesToApiTab`.
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.** O lado esquerdo ainda é um placeholder; a página monta `TopBar` + `SplitLayout`.
- [ ] **Step 4: Verificar** — `npm test`, lint, tsc; `npm run dev` e conferir no navegador em 1280 px e 375 px.
- [ ] **Step 5: Commit (após ok)** — `feat: tela dividida com painel da linha do tempo`

---

### Task 4: Preenchimento automático e cadastro descartável

**Files:**
- Create: `web/src/hooks/useTypewriter.ts`
- Create: `web/src/features/onboarding/Onboarding.tsx`, `web/src/features/onboarding/LockScreen.tsx`, `web/src/features/onboarding/demoIdentity.ts`
- Create: `web/src/app/DemoApp.tsx` (`'use client'`: decide entre `Onboarding` e o sistema conforme `getToken()`; registra `configureApi({ onUnauthorized })`; **é o dono do estado `waiting`** e passa o setter para `Onboarding` e, na Task 5, para o `SystemShell`; monta `SplitLayout` com `TimelinePanel waiting={waiting}`)
- Test: `web/src/hooks/useTypewriter.test.ts`, `web/src/features/onboarding/Onboarding.test.tsx`, `web/src/app/DemoApp.test.tsx`

**Interfaces:**
- Consumes: `api`, `ApiError`, `isDailyLimit`, `configureApi` (Task 2); `setToken`/`getToken`/`clearToken`; `clearTimeline` não é chamado (o histórico continua visível).
- Produces:
  - `useTypewriter<F extends Record<string, string>>(target: F, opts?: { charDelayMs?: number }): { values: F; setValue: (field: keyof F, value: string) => void; done: boolean; typing: boolean; restart: (next: F) => void }` — preenche campo por campo, na ordem das chaves, `charDelayMs` padrão **25**; `setValue` (edição do visitante) para a animação na hora e marca `done`. Inputs `select`/`date` usam o mesmo hook (valor aparece de uma vez quando chega a vez do campo).
  - `demoIdentity(): { name: string; email: string; password: string }` — `name` "Visitante NNNN" (4 dígitos), `email` `visitante-<timestamp36><rand>@demo.thiagoheckler.com.br`, `password` 12 caracteres alfanuméricos.
  - `Onboarding({ onLoggedIn }: { onLoggedIn: () => void })`: formulário preenchido pelo typewriter; texto "Conta temporária de demonstração, apagada em 24 h"; botão **"Criar conta e entrar"** (pulsando quando `done`); ao clicar: `POST /users` e, se 201, `POST /auth/login`; guarda o token e chama `onLoggedIn`. Erro 400 mostra `fields` no campo. Enquanto não clica, `TimelinePanel` recebe `waiting = true`.
  - `LockScreen`: tela que substitui a demo com a mensagem exata do teto diário, o `Retry-After` convertido em "volta a funcionar à meia-noite (horário de Brasília)" e o link "Ver somente API".
  - 429 por IP (não diário): aviso inline "Muitas requisições. Aguarde um minuto e tente novamente." sem travar.
- [ ] **Step 1: Testes que falham** (fake timers do Vitest para o typewriter):
  - `useTypewriter.test.ts` → `typesFieldsInOrder`, `marksDoneAtEnd`, `stopsWhenUserEdits` (depois do `setValue`, avançar o tempo não muda mais nada e o valor é o editado).
  - `Onboarding.test.tsx` → `waitsForClickBeforeCallingApi` (nenhum `fetch` após a animação terminar), `createsUserThenLogsIn` (ordem: `POST /api/users` depois `POST /api/auth/login`; token salvo; `onLoggedIn` chamado), `showsLockScreenOnDailyLimit`, `showsWaitWarningOnIpLimit`, `showsFieldErrorsOn400`.
  - `DemoApp.test.tsx` → `unauthorizedReturnsToOnboarding` (com token salvo, uma chamada que volta 401 limpa token e cache e mostra o cadastro com aviso "Sua sessão expirou. Crie uma nova conta de demonstração.").
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Verificar** — testes, lint, tsc; no navegador contra a API local: cadastro aparece digitando, para, clicar cria conta + login e a linha do tempo mostra as duas chamadas com o token na resposta do login.
- [ ] **Step 5: Commit (após ok)** — `feat: cadastro descartável com preenchimento automático`

---

### Task 5: Sistema — navegação, Categorias e Tags

**Files:**
- Create: `web/src/features/system/SystemShell.tsx` (navegação lateral: Tarefas, Categorias, Tags, Minha conta; seção ativa em `useState`)
- Create: `web/src/features/categories/{api.ts,CategoriesPage.tsx,CategoryForm.tsx}`, `web/src/features/tags/{api.ts,TagsPage.tsx,TagForm.tsx}`
- Create: `web/src/features/presets.ts` (dados de exemplo usados pelo typewriter)
- Test: `web/src/features/categories/CategoriesPage.test.tsx`, `web/src/features/tags/TagsPage.test.tsx`

**Interfaces:**
- Consumes: `api`, `ApiError` (Task 2); `useTypewriter` (Task 4); `TimelinePanel waiting` — o shell expõe `onWaitingChange(waiting: boolean)` para a página, `true` enquanto um formulário foi preenchido e não foi salvo.
- Produces:
  - Query keys: `['categories']`, `['tags']`. Lista carregada ao abrir a seção (uma chamada `GET` visível).
  - Fluxo comum: "Nova …" abre o formulário e dispara o typewriter com o próximo preset; "Salvar" faz `POST`; cada item da lista tem **Editar** (formulário com os valores atuais, typewriter preenche só a mudança sugerida) → `PUT /…/{id}` e **Excluir** (confirmação inline) → `DELETE /…/{id}`; após qualquer mutação, `invalidateQueries` da chave (gera o `GET` seguinte, que é real e aparece na linha do tempo).
  - `presets.ts`: categorias `[{name:'Estudos', color:'#4f46e5'}, {name:'Trabalho', color:'#0ea5e9'}, {name:'Pessoal', color:'#22c55e'}]`; tags `['urgente', 'leitura', 'backend']`; ao esgotar, sufixo numérico (`Estudos 2`) para não bater no 409 de nome duplicado. O 409 continua possível se o visitante digitar um nome repetido — e aparece como erro real.
  - Erros: 400 → mensagem no campo (`fields`); 409 → mensagem acima do formulário.
- [ ] **Step 1: Testes que falham** → `CategoriesPage.test.tsx`: `listsCategoriesOnOpen`, `createsCategoryWithPreset` (POST com `{name:'Estudos', color:'#4f46e5'}` depois GET), `showsConflictMessageOn409`, `deletesAfterConfirmation`. `TagsPage.test.tsx`: `createsTagWithPreset`, `editsTag` (PUT `/api/tags/{id}`).
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Verificar** — testes, lint, tsc; no navegador: criar, editar e excluir categoria e tag, conferindo cada chamada no painel.
- [ ] **Step 5: Commit (após ok)** — `feat: categorias e tags na demo`

---

### Task 6: Tarefas e Minha conta

**Files:**
- Create: `web/src/features/tasks/{api.ts,TasksPage.tsx,TaskForm.tsx,TaskFilters.tsx}`
- Create: `web/src/features/account/AccountPage.tsx`
- Test: `web/src/features/tasks/TasksPage.test.tsx`, `web/src/features/account/AccountPage.test.tsx`

**Interfaces:**
- Consumes: Tasks 2, 4, 5 (`api`, `useTypewriter`, `presets`, query keys `['categories']`/`['tags']` para preencher os selects).
- Produces:
  - Query keys: `['tasks', filter]` onde `filter` é `'all' | 'root' | TaskStatus`; filtros mapeiam para `GET /tasks`, `GET /tasks/root`, `GET /tasks?status=…`.
  - `TaskForm` (criar): typewriter preenche título, descrição, prioridade, prazo (`datetime-local`, convertido com `toApiDateTime`); se já existir categoria, escolhe a primeira; se existirem tags, marca a primeira. Campo opcional "Subtarefa de" (select com tarefas principais) — `parentTaskId`. Presets de tarefas em `presets.ts` (ex.: "Estudar Spring Security", "Revisar o README", "Gravar vídeo da demo").
  - Lista: cada tarefa mostra título, prioridade, status, prazo e `completedAt` (`formatApiDate`), categoria, tags e subtarefas indentadas sob a principal. Ações: **Concluir** (`PUT` com `{status:'COMPLETED'}` — a resposta traz `completedAt` preenchido), **Reabrir** (`{status:'PENDING'}`), **Editar** (`PUT` parcial), **Excluir** (aviso: "As subtarefas são excluídas junto").
  - 422 ao tentar criar subtarefa de subtarefa aparece como erro real no formulário.
  - `AccountPage`: `GET /users/me` ao abrir; editar nome (`PUT /users/me` com `{name}`); **Excluir conta** (confirmação) → `DELETE /users/me` → `clearToken()`, `queryClient.clear()`, volta ao cadastro.
- [ ] **Step 1: Testes que falham** → `TasksPage.test.tsx`: `createsTaskWithPresetAndConvertsDueDateToUtc`, `completingShowsCompletedAt`, `filterByStatusCallsQueryParam`, `rootFilterCallsRootEndpoint`, `showsBusinessRuleErrorOn422`. `AccountPage.test.tsx`: `updatesName`, `deletingAccountReturnsToOnboarding`.
- [ ] **Step 2: Rodar e ver falhar.**
- [ ] **Step 3: Implementar.**
- [ ] **Step 4: Verificar** — testes, lint, tsc, `npm run build`; no navegador, roteiro completo contra a API local: cadastro → categoria → tag → tarefa com categoria e tag → subtarefa → concluir → filtrar → excluir conta.
- [ ] **Step 5: Commit (após ok)** — `feat: tarefas e conta na demo`

---

### Task 7: Fechamento da parte 2

**Files:**
- Create: `web/README.md` (como rodar: API na raiz + `npm run dev` em `web/`; por que export estático; onde fica o proxy de dev)
- Modify: `README.md` da raiz (seção da demo e da pasta `web/`), `taskapi.md` (estrutura, decisões do front, fase 11, próximo passo = Parte 3)

- [ ] **Step 1: Teste manual com limites baixos**: API com `DEMO_SIGNUP_DAILY_LIMIT=1`; segundo cadastro na demo mostra a `LockScreen`; com `DEMO_RATE_LIMIT_PER_MINUTE=5`, cliques rápidos mostram o aviso de espera sem travar; "Ver somente API" abre o Swagger. (No dev, o Swagger via `/api` pode não carregar o `api-docs` porque o proxy do Next não envia `X-Forwarded-Prefix`; em produção o Caddy envia. Se acontecer, registrar no `web/README.md`.)
- [ ] **Step 2: Atualizar a documentação** conforme Files.
- [ ] **Step 3: Verificação final** — `npm test`, `npm run lint`, `npx tsc --noEmit`, `npm run build` em `web/`; `./mvnw test` na raiz continua verde.
- [ ] **Step 4: Commit (após ok)** — `docs: documenta o front da demo`
