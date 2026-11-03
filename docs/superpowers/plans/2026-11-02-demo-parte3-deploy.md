# Demo tela dividida — Parte 3: deploy — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publicar a demo em `https://tasks.thiagoheckler.com.br`: site estático em `/`, API em `/api`, Swagger em `/api/swagger-ui/index.html`, rodando na VPS dentro da stack do repo `Portfolio`.

**Architecture:** A API vira a imagem `thiagoheckler/task-api` (multi-stage), buildada no PC do Thiago e publicada no Docker Hub; a VPS só faz `pull`. O front (`web/out/`) é copiado por `rsync` para uma pasta montada no Caddy do Portfolio, que serve os arquivos em `/` e repassa `/api/*` para o container sem o prefixo, sobrescrevendo `X-Forwarded-For` e enviando `X-Forwarded-Prefix /api`. O schema nasce pelo Flyway na primeira subida, no PostgreSQL 17 compartilhado.

**Tech Stack:** Docker (multi-stage `eclipse-temurin:25`), Docker Hub, Docker Compose e Caddy 2 do repo `Portfolio`, PostgreSQL 17, Spring profiles (`prod`), Next.js 16 export estático, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-28-demo-tela-dividida-design.md` (§5 Restrições, §9 Deploy). Partes 1 e 2 prontas na branch `feat/demo-api`. Infra: `~/Público/wsdev/trabalhos/Portfolio` (`CLAUDE.md`, `docker-compose.yml`, `caddy/Caddyfile`).

## Global Constraints

- Dois repositórios: **Task_API** (branch `feat/demo-api`) e **Portfolio** (criar branch `feat/task-api-deploy` a partir de `main`). `git fetch` nos dois antes de começar.
- Decisões do Thiago (02/11/2026): imagem **buildada localmente e publicada no Docker Hub** (`thiagoheckler/task-api`, tags `latest` e o SHA curto do commit); **`application.yml` passa a ser versionado** (sem segredo) e `application-prod.yml` exige `JWT_SECRET` sem fallback; a stack do Portfolio **já está no ar** na VPS (Caddy é dono de 80/443, PostgreSQL 17 existe).
- Restrições do Portfolio (`CLAUDE.md` de lá), sem mudar: serviço herda `x-java-app` (384 MB, `-Xmx160m`, SerialGC); porta interna **8080**; HikariCP `maximum-pool-size: 5`; bancos sem porta exposta; **o serviço `task-api` não publica porta** (requisito do limite por IP).
- Caddy do `tasks.`: `header_up X-Forwarded-For {remote_host}` e `header_up X-Forwarded-Prefix /api`. Sem isso o limite por IP é burlável e o Swagger gera URLs sem `/api`.
- Segredos só no `.env` da VPS. O `JWT_SECRET` da Task API é **próprio** (`TASKAPI_JWT_SECRET`), não o `JWT_SECRET` do auth-service. Gerar com `openssl rand -base64 48`.
- Nenhuma dependência nova no `pom.xml` nem no `web/package.json`.
- **Ações externas** (`docker push`, `ssh`/`rsync` na VPS, `git push`, PR) são feitas pelo Thiago ou só com o ok dele no momento. O plano lista os comandos; o executor não roda sem ok.
- Commits em português, Conventional Commits, com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, cada um depois do ok do Thiago na task.
- Verificação Task_API: `./mvnw test` (banco local `docker compose up -d`, porta 5440). Verificação front, em `web/`: `npm test`, `npm run lint`, `npx tsc --noEmit`, `npm run build`.

## Review Focus

- **`X-Forwarded-For` forjado pelo visitante:** 130 requisições com XFF trocando a cada chamada devem levar 429 do mesmo jeito (o Caddy sobrescreve). → Task 4, Step 6.
- **Produção subindo com o segredo de desenvolvimento:** com `SPRING_PROFILES_ACTIVE=prod` e sem `JWT_SECRET`, a API não sobe. → Task 2, Step 6.
- **Swagger atrás do prefixo:** "Ver somente API" abre e o "Try it out" chama `https://tasks.thiagoheckler.com.br/api/...` (não `/users` na raiz, não `http`). → Task 4, Step 7.
- **Banco `taskapi` na VPS já com tabelas antigas (sem `flyway_schema_history`):** o Flyway falharia ou o `validate` quebraria. Checar antes da primeira subida e parar se houver tabelas. → Task 4, Step 2.
- **Recarregar uma rota do site ou pedir arquivo inexistente:** o Caddy responde o `404.html` do export, sem cair em erro do Caddy nem repassar à API. → Task 4, Step 7.

---

### Task 1: Pré-deploy — corrigir dois achados do front que o visitante vê

Achados #8 e #11 da revisão final (ledger `.superpowers/sdd/2026-10-29-demo-parte2-front/progress.md`).

- **#8:** na edição, limpar prazo ou categoria não faz nada (a API trata ausente/`null` como "não alterar"; `toUpdateInput` descarta vazio). A API não muda. O front para de oferecer a ação que não existe.
- **#11:** um ano de 6 dígitos no `datetime-local` (ex.: `275760-09-14T00:00`) faz `toISOString()` lançar `RangeError` e derruba a página. Em vez de validar no cliente, o valor vai cru e a API devolve o 400 real (a validação mostrada é a da API).

**Files:**
- Modify: `web/src/lib/dates.ts` (`toApiDateTime`)
- Modify: `web/src/features/tasks/TaskForm.tsx` (opção "Sem categoria" e input de prazo)
- Test: `web/src/lib/dates.test.ts`, `web/src/features/tasks/TaskForm.test.tsx` (novo)

**Interfaces:**
- Produces: `toApiDateTime(local: string): string` — mesmo contrato; para data inválida devolve `local` sem mudar.

- [ ] **Step 1: Testes que falham**

```ts
// dates.test.ts
it('passes an invalid date through unchanged so the API answers with its own 400', () => {
  expect(() => toApiDateTime('275760-09-14T00:00')).not.toThrow()
  expect(toApiDateTime('275760-09-14T00:00')).toBe('275760-09-14T00:00')
})

// TaskForm.test.tsx — render <TaskForm mode="edit" current={…} suggested={…} …/> com
// current.dueDate = '2026-11-09T18:00' e current.categoryId = '1', categories = [{ id: 1, name: 'Estudos' }]
it('does not offer clearing the category when editing a task that has one', () => {
  expect(screen.getByRole('option', { name: 'Sem categoria' })).toBeDisabled()
})
it('requires a due date when editing a task that has one', () => {
  expect(screen.getByLabelText('Prazo')).toBeRequired()
})
it('keeps both optional when creating', () => {
  // mode="create", sem current
  expect(screen.getByRole('option', { name: 'Sem categoria' })).toBeEnabled()
  expect(screen.getByLabelText('Prazo')).not.toBeRequired()
})
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd web && npx vitest run src/lib/dates.test.ts src/features/tasks/TaskForm.test.tsx`
Expected: FAIL — `RangeError: Invalid time value` e as asserções de `disabled`/`required`.

- [ ] **Step 3: Implementar**

`toApiDateTime`: se `Number.isNaN(asUtc.getTime())`, retorna `local`. `TaskForm`: `disabled` na opção `""` de categoria quando `mode === 'edit' && current?.categoryId`; `required` no input de prazo quando `mode === 'edit' && current?.dueDate` (o `<form>` não tem `noValidate`, então o navegador bloqueia o envio vazio).

- [ ] **Step 4: Verificar**

Run: `cd web && npm test && npm run lint && npx tsc --noEmit && npm run build`
Expected: tudo verde (65+ testes), `web/out/index.html` gerado.

- [ ] **Step 5: Conferir no navegador** (`npm run dev` + API local): editar tarefa com prazo → apagar o prazo → Salvar é barrado pelo navegador; digitar ano `275760` numa tarefa nova → a linha do tempo mostra o 400 da API e a página continua de pé.

- [ ] **Step 6: Commit**

```bash
git add web/src/lib/dates.ts web/src/lib/dates.test.ts web/src/features/tasks/TaskForm.tsx web/src/features/tasks/TaskForm.test.tsx
git commit -m "fix: data inválida não derruba a demo e edição não promete limpar prazo/categoria"
```

---

### Task 2: Imagem de produção da API, validada no PostgreSQL 17

**Files:**
- Modify: `.gitignore` (remover a linha `*application.yml`)
- Create (versionar): `src/main/resources/application.yml` — conteúdo do `applicationExemple.yml` atual (já usa `${DB_USER:thiago}`, `${JWT_SECRET:fallback-dev}`, `${DEMO_…}`)
- Delete: `src/main/resources/applicationExemple.yml`
- Create: `src/main/resources/application-prod.yml`
- Create: `Dockerfile`, `.dockerignore`
- Modify: `taskapi.md` / `README.md` — trocar "copiar `applicationExemple.yml`" por "o `application.yml` já vem pronto para o banco do `docker-compose.yml`"

**Interfaces:**
- Produces: imagem `thiagoheckler/task-api:latest` escutando em **8080** com `SPRING_PROFILES_ACTIVE=prod`. Variáveis lidas: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` (obrigatória no `prod`), `DEMO_SIGNUP_DAILY_LIMIT`, `DEMO_RATE_LIMIT_PER_MINUTE`.

- [ ] **Step 1: Conferir que o `application.yml` local só difere do Exemple nos valores de segredo**

Run: `diff src/main/resources/application.yml src/main/resources/applicationExemple.yml`
Expected: no máximo as linhas de `password`/`secret`. Se o local tiver segredo real, **não** versionar o local: versionar o Exemple renomeado.

- [ ] **Step 2: `application-prod.yml`** com exatamente estes valores:

```yaml
server:
  port: 8080
spring:
  jpa:
    show-sql: false
    properties:
      hibernate:
        format_sql: false
  datasource:
    hikari:
      maximum-pool-size: 5
jwt:
  secret: ${JWT_SECRET}
```

- [ ] **Step 3: `Dockerfile` multi-stage**

Build em `maven:3.9-eclipse-temurin-25` (`./mvnw` não precisa: usar `mvn -B -q package -DskipTests` depois de `mvn -B dependency:go-offline` com só o `pom.xml` copiado, para cache de camadas). Runtime em `eclipse-temurin:25-jre-alpine`, usuário não-root, `EXPOSE 8080`, `ENTRYPOINT ["java","-jar","/app/app.jar"]`. Sem `JAVA_TOOL_OPTIONS` na imagem (vêm do `x-java-app`). Testes ficam fora do build porque o `contextLoads` precisa de banco; rodam no Step 4.

`.dockerignore`: `web/`, `target/`, `.git/`, `.superpowers/`, `.claude-flow/`, `.swarm/`, `.playwright-mcp/`, `.ai-jail`, `ruvector.db`, `docs/`, `*.md`, `.env`.

- [ ] **Step 4: Testes da API**

Run: `docker compose up -d && ./mvnw test`
Expected: `Tests run: 19, Failures: 0, Errors: 0`.

- [ ] **Step 5: Build da imagem**

Run: `docker build -t thiagoheckler/task-api:latest -t thiagoheckler/task-api:$(git rev-parse --short HEAD) .`
Expected: build ok; `docker image ls thiagoheckler/task-api` mostra as duas tags.

- [ ] **Step 6: Smoke no PostgreSQL 17 com os limites da VPS**

```bash
SCRATCH=$(mktemp -d)   # pasta temporária fora do repo
docker network create taskapi-smoke
docker run -d --name smoke-pg --network taskapi-smoke -e POSTGRES_DB=taskapi \
  -e POSTGRES_USER=smoke -e POSTGRES_PASSWORD=smoke postgres:17-alpine
# smoke.env no scratchpad (fora do repo); valores iguais aos do x-java-app
cat > "$SCRATCH/smoke.env" <<'EOF'
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://smoke-pg:5432/taskapi
SPRING_DATASOURCE_USERNAME=smoke
SPRING_DATASOURCE_PASSWORD=smoke
JAVA_TOOL_OPTIONS=-Xmx160m -Xms64m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -Xss512k -XX:MaxDirectMemorySize=32m
EOF
# 6a. sem JWT_SECRET: tem que falhar
docker run --rm --network taskapi-smoke --env-file "$SCRATCH/smoke.env" --memory 384m thiagoheckler/task-api:latest
# 6b. com JWT_SECRET: tem que subir
docker run -d --name smoke-api --network taskapi-smoke --env-file "$SCRATCH/smoke.env" --memory 384m \
  -e JWT_SECRET="$(openssl rand -base64 48)" -p 127.0.0.1:18080:8080 thiagoheckler/task-api:latest
```

Expected:
- 6a: container sai com erro contendo `Could not resolve placeholder 'JWT_SECRET'`.
- 6b: log com `Successfully applied 1 migration` e `Started TaskApiApplication`; `POST /users` → 201, `POST /auth/login` → 200 com `token`, `GET /tasks` com Bearer → 200 (via `curl` em `127.0.0.1:18080`); criar uma subtarefa de subtarefa → 422 (a trigger existe no 17); `docker stats --no-stream smoke-api` abaixo de 384 MiB.
- Limpeza: `docker rm -f smoke-api smoke-pg && docker network rm taskapi-smoke`.

- [ ] **Step 7: Commit**

```bash
git add .gitignore src/main/resources/application.yml src/main/resources/application-prod.yml Dockerfile .dockerignore taskapi.md README.md
git rm src/main/resources/applicationExemple.yml
git commit -m "feat: imagem Docker de produção da API com perfil prod"
```

---

### Task 3: Portfolio — serviço `task-api`, Caddy do `tasks.` e `.env.example`

Repo `~/Público/wsdev/trabalhos/Portfolio`, branch `feat/task-api-deploy`.

**Files:**
- Modify: `docker-compose.yml` (serviço `task-api`; volume do site da demo no `caddy`)
- Modify: `caddy/Caddyfile` (bloco `tasks.thiagoheckler.com.br`)
- Create: `.env.example` (o README cita, mas não existe)
- Modify: `.gitignore` (`sites/tasks/`)
- Modify: `README.md` (seção "Deploy da Task API")

**Interfaces:**
- Consumes: imagem e variáveis da Task 2.
- Produces: pasta `sites/tasks/` na raiz do Portfolio (fora do git) montada em `/srv/tasks` no Caddy; variáveis `TASKAPI_JWT_SECRET`, `TASKAPI_SIGNUP_DAILY_LIMIT`, `TASKAPI_RATE_LIMIT_PER_MINUTE` no `.env`.

- [ ] **Step 1: Serviço `task-api`**

Herdar `*java-app` e `*java-env`; **sobrescrever `depends_on` só com `postgres: service_healthy`** (a âncora inclui o mongo, que a Task API não usa e que gastaria RAM se não estivesse de pé). Ambiente além do atual: `SPRING_PROFILES_ACTIVE: prod`, `JWT_SECRET: ${TASKAPI_JWT_SECRET:?defina TASKAPI_JWT_SECRET no .env}`, `DEMO_SIGNUP_DAILY_LIMIT: ${TASKAPI_SIGNUP_DAILY_LIMIT:-100}`, `DEMO_RATE_LIMIT_PER_MINUTE: ${TASKAPI_RATE_LIMIT_PER_MINUTE:-120}`. Sem `ports:`.

- [ ] **Step 2: Caddy** — volume `./sites/tasks:/srv/tasks:ro` no serviço `caddy`; bloco do `tasks.`:

```caddy
tasks.thiagoheckler.com.br {
	import log_acesso
	encode gzip
	root * /srv/tasks

	# API: remove /api; o IP do visitante vem do Caddy, nunca do cliente
	handle_path /api/* {
		reverse_proxy task-api:8080 {
			header_up X-Forwarded-For {remote_host}
			header_up X-Forwarded-Prefix /api
		}
	}

	# Front estático (Next.js export)
	handle {
		try_files {path} {path}.html
		file_server
	}

	@static path /_next/static/*
	header @static Cache-Control "public, max-age=31536000, immutable"

	handle_errors 404 {
		rewrite * /404.html
		file_server
	}
}
```

- [ ] **Step 3: `.env.example`** — todas as variáveis que o compose usa, sem valores reais: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `MONGO_USER`, `MONGO_PASSWORD`, `JWT_SECRET` (auth-service), `TASKAPI_JWT_SECRET` (comentário: `openssl rand -base64 48`), `TASKAPI_SIGNUP_DAILY_LIMIT=100`, `TASKAPI_RATE_LIMIT_PER_MINUTE=120`.

- [ ] **Step 4: Validar**

Run: `docker compose --env-file .env.example config -q`
Expected: falha com `defina TASKAPI_JWT_SECRET no .env` (o example deixa o valor vazio de propósito).

Run: `TASKAPI_JWT_SECRET=x docker compose --env-file .env.example config -q`
Expected: sem saída, exit 0.

Run: `docker run --rm -v "$PWD/caddy/Caddyfile:/etc/caddy/Caddyfile:ro" caddy:2-alpine caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile`
Expected: `Valid configuration`.

- [ ] **Step 5: README** — seção curta "Deploy da Task API" apontando para `scripts/deploy.sh` do repo Task_API e para o rollback (Task 4, Step 8).

- [ ] **Step 6: Commit**

```bash
git add docker-compose.yml caddy/Caddyfile .env.example .gitignore README.md
git commit -m "feat: publica a demo da Task API em tasks.thiagoheckler.com.br"
```

---

### Task 4: Primeiro deploy na VPS e verificação em produção

Tudo aqui é ação externa: o Thiago roda, ou dá ok comando a comando. `VPS=dev@72.60.49.243`; `PORTFOLIO_DIR` = pasta do Portfolio na VPS (confirmar no Step 1).

**Files:**
- Create: `scripts/deploy.sh` (Task_API) — redeploy repetível

**Interfaces:**
- Consumes: Tasks 1–3 commitadas; `docker login` feito no PC.

- [ ] **Step 1: Estado da VPS**

Run: `ssh "$VPS" "cd '$PORTFOLIO_DIR' && git status -sb && docker compose ps && free -m"`
Expected: Portfolio em `main` limpo, `caddy` e `postgres` `Up (healthy)`; memória livre ≥ 450 MB para os 384 MB da API. Se não houver folga, parar e decidir com o Thiago.

- [ ] **Step 2: Banco `taskapi`**

Run: `ssh "$VPS" "cd '$PORTFOLIO_DIR' && docker compose exec -T postgres sh -c 'psql -U \"\$POSTGRES_USER\" -d taskapi -c \"\\dt\"'"`
(o `$POSTGRES_USER` é expandido dentro do container, onde a variável existe.)
Expected: `Did not find any relations.` Se o banco não existir: `docker compose exec -T postgres sh -c 'createdb -U "$POSTGRES_USER" taskapi'` na VPS. **Se houver tabelas, parar** (sem `DROP` sem ok do Thiago).

- [ ] **Step 3: `scripts/deploy.sh`** (bash, `set -euo pipefail`; `VPS` e `PORTFOLIO_DIR` por variável de ambiente, sem default de caminho): (1) recusa rodar com árvore suja; (2) `docker build` com tags `latest` e SHA curto; (3) `docker push` das duas; (4) `npm --prefix web ci && npm --prefix web run build`; (5) `rsync -a --delete web/out/ $VPS:$PORTFOLIO_DIR/sites/tasks/`; (6) `ssh $VPS "cd $PORTFOLIO_DIR && docker compose pull task-api && docker compose up -d task-api"`. Imprime o SHA publicado no fim. Verificar com `bash -n scripts/deploy.sh` e `shellcheck scripts/deploy.sh` (se instalado).

- [ ] **Step 4: Preparar a VPS** (uma vez): `git pull` do Portfolio na branch mergeada (ou `git checkout feat/task-api-deploy`), adicionar `TASKAPI_JWT_SECRET` ao `.env`, `mkdir -p sites/tasks`, `docker compose up -d caddy` (recria o Caddy para montar o volume: os sites ficam fora por alguns segundos).

- [ ] **Step 5: Deploy**

Run: `VPS=… PORTFOLIO_DIR=… scripts/deploy.sh` e depois `ssh $VPS 'cd $PORTFOLIO_DIR && docker compose logs --tail 50 task-api'`
Expected: `Successfully applied 1 migration`, `Started TaskApiApplication`; `docker compose ps task-api` sem `0.0.0.0:` na coluna de portas.

- [ ] **Step 6: Limite por IP não burlável**

Run: `for i in $(seq 1 130); do curl -s -o /dev/null -w '%{http_code}\n' -H "X-Forwarded-For: 10.0.0.$i" https://tasks.thiagoheckler.com.br/api/tasks; done | sort | uniq -c`
Run de novo, depois de 1 min, com `-H "Forwarded: for=10.0.0.$i"` no lugar do `X-Forwarded-For` (o Spring lê o `Forwarded` primeiro; o Caddy remove esse header).
Expected: ~120 × `401` e o resto `429`. Se nenhum 429 aparecer, o XFF forjado está passando: **tirar a demo do ar** (`docker compose stop task-api`) antes de investigar.

- [ ] **Step 7: Roteiro no navegador** (aba anônima, `https://tasks.thiagoheckler.com.br`): cadastro → token abreviado na linha do tempo → categoria → tag → tarefa → concluir (`completedAt` em horário de SP) → excluir conta. "Ver somente API" abre o Swagger, e o "Try it out" de `POST /users` chama `https://tasks.thiagoheckler.com.br/api/users`. `https://tasks.thiagoheckler.com.br/nao-existe` mostra o 404 do Next. Certificado válido. Em `https://thiagoheckler.com.br` e nos outros subdomínios nada mudou.

- [ ] **Step 8: Rollback documentado** (no `scripts/deploy.sh` como comentário e no README do Portfolio): `docker compose pull` não serve para voltar; usar `docker pull thiagoheckler/task-api:<sha-anterior> && docker tag thiagoheckler/task-api:<sha-anterior> thiagoheckler/task-api:latest && docker compose up -d task-api`. O front volta rodando o `deploy.sh` a partir do commit anterior. Migrations Flyway não voltam: imagem antiga só é segura se não houver migration nova entre as duas.

- [ ] **Step 9: Commit**

```bash
git add scripts/deploy.sh
git commit -m "chore: script de deploy da demo na VPS"
```

---

### Task 5: Fechamento — docs, PRs e vault

**Files:**
- Modify: `README.md` (link da demo e do Swagger em produção no topo), `taskapi.md` (seção de deploy: imagem, perfil `prod`, variáveis, `scripts/deploy.sh`, rollback)

- [ ] **Step 1: Docs** — README e `taskapi.md` como acima. Commit: `docs: link da demo em produção e roteiro de deploy`.
- [ ] **Step 2: PRs** (Thiago dá o ok): push de `feat/demo-api` → PR para `main` no Task_API; push de `feat/task-api-deploy` → PR para `main` no Portfolio. Depois do merge, a VPS volta para `main` do Portfolio (`git checkout main && git pull`).
- [ ] **Step 3: Vault** (perguntar antes de gravar): nota `Task API` (Parte 3 concluída, status, decisões: Docker Hub + `application.yml` versionado + perfil `prod` + `TASKAPI_JWT_SECRET` próprio; problemas e soluções que surgirem), nota `Portfolio Spring` (Task API no ar, passo 3 da ordem de trabalho do `CLAUDE.md` concluído) e linha no `Registro de decisoes`.
