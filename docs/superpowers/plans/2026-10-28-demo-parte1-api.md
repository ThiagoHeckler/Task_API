# Demo tela dividida — Parte 1: API — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preparar a Task API para ficar pública atrás da demo: schema via Flyway, teto diário de cadastros, limite por IP, limpeza das contas demo e funcionamento atrás do proxy em `/api`.

**Architecture:** Mudanças só no backend Spring existente, nas camadas que já são donas de cada regra: teto diário no `UserService`, limite por IP como filtro na cadeia do Spring Security (como o `JwtAuthenticationFilter`), limpeza como job agendado chamando o `UserRepository`, prefixo `/api` resolvido por forwarded headers (o Caddy remove o prefixo). Contagem e limpeza usam `now()` do próprio PostgreSQL para não depender do fuso da JVM.

**Tech Stack:** Java 25, Spring Boot 4.1.0, Spring Security, Spring Data JPA, PostgreSQL 18 local / 17 na VPS, Flyway, springdoc 3.1.1, JUnit 5 + Mockito + MockMvc.

**Spec:** `docs/superpowers/specs/2026-10-28-demo-tela-dividida-design.md` (seções 5, 6, 7.5, 8). Partes 2 (front Next.js) e 3 (deploy) terão planos próprios, escritos depois desta.

## Global Constraints

- Pacote base `com.thiago.taskapi.task_api`; indentação com tab; comentários e mensagens ao usuário em português; nomes no código em inglês.
- Sem Lombok. DTOs em records. Regras de negócio nos services, não nos controllers.
- Todo erro sai como `ErrorResponse(Instant timestamp, int status, String error, String message)`.
- Dependências novas permitidas neste plano: só `spring-boot-starter-flyway` e `org.flywaydb:flyway-database-postgresql` (o starter traz o `flyway-core` no Boot 4). Nenhuma outra.
- Valores do spec: teto diário padrão **100**; contas apagadas com **mais de 24 h**; fuso do "dia" **`America/Sao_Paulo`**; mensagem do teto exatamente **"Limite de usuários que testaram o sistema hoje atingido"**.
- Configuração nova via variável de ambiente com padrão no yml; segredos nunca no código. Atualizar `applicationExemple.yml` em toda task que criar propriedade (o `application.yml` local está no `.gitignore`; copiar a mudança para ele também).
- Testes de integração usam o PostgreSQL local do `docker-compose.yml` (porta 5440), como o `contextLoads` já faz. Subir com `docker compose up -d --wait` antes de `./mvnw test`.
- Commits em português, Conventional Commits, terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. **Cada commit só depois do ok do Thiago na task** (ele valida etapa por etapa).

## Review Focus

- **Virada do dia em São Paulo, não em UTC:** cadastro às 23:59 de ontem (horário de SP) não conta no teto de hoje; às 00:01 conta. → teste `countCreatedToday_ignoresYesterdayInSaoPaulo` na Task 2.
- **Front precisa distinguir os dois 429:** o do teto diário (trava a demo) e o do limite por IP (espere um pouco). Mensagens diferentes e `Retry-After` presente nos dois. → testes `create_returns429WithDailyLimitMessage` (Task 2) e `exceedingLimit_returns429ErrorResponse` (Task 3).
- **Visitantes atrás do mesmo proxy não podem dividir o mesmo limite:** o IP vem do `X-Forwarded-For` que o Caddy define; se o forwarding falhar, todo o site viraria um IP só. → teste `differentForwardedClients_haveSeparateLimits` na Task 3.
- **Swagger UI não pode quebrar por causa do limitador:** abrir o Swagger baixa vários arquivos estáticos; recarregar algumas vezes não deve dar 429 nos assets. Decisão nesta revisão: `/swagger-ui/**` (estáticos) fica fora do limite; `/v3/api-docs` e o resto contam. → teste `swaggerStaticAssets_areNotLimited` na Task 3.
- **Limpeza só apaga conta velha, e leva os dados junto:** conta com 25 h some com suas tarefas (cascade); conta com 1 h fica. → teste `deleteOlderThan24Hours_removesOnlyOldUsersAndCascades` na Task 4.

---

### Task 1: Flyway assume o schema

**Files:**
- Modify: `pom.xml` (dependências)
- Move: `src/main/resources/schema.sql` → `src/main/resources/db/migration/V1__schema_inicial.sql` (com `git mv`, conteúdo inalterado)
- Modify: `docker-compose.yml` (remover o volume `./src/main/resources/schema.sql:/docker-entrypoint-initdb.d/schema.sql:ro`)
- Modify: `src/main/resources/applicationExemple.yml` (nada novo é obrigatório; `ddl-auto: validate` continua)
- Modify: `README.md` e `taskapi.md` onde citam `schema.sql` / recriar banco
- Test: `src/test/java/com/thiago/taskapi/task_api/FlywayMigrationTest.java`

**Interfaces:**
- Produces: tabela `flyway_schema_history` com a versão `1`; local das próximas migrations: `src/main/resources/db/migration/V{n}__descricao.sql`.

- [ ] **Step 1: Escrever o teste que falha**

```java
@SpringBootTest
class FlywayMigrationTest {
	@Autowired JdbcTemplate jdbcTemplate;

	@Test
	void v1IsApplied() {
		Integer applied = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
		assertThat(applied).isEqualTo(1);
	}
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw test -Dtest=FlywayMigrationTest`
Expected: FAIL — `relation "flyway_schema_history" does not exist`.

- [ ] **Step 3: Adicionar as dependências e mover o schema**

`spring-boot-starter-flyway` (versão gerenciada pelo Boot) e `org.flywaydb:flyway-database-postgresql` (versão gerenciada pelo Boot). `git mv` do `schema.sql` para `db/migration/V1__schema_inicial.sql`. Remover do `docker-compose.yml` o volume que montava o `schema.sql` no `docker-entrypoint-initdb.d`.

- [ ] **Step 4: Recriar o banco local — PEDIR CONFIRMAÇÃO AO THIAGO ANTES**

O banco local atual foi criado pelo `docker-entrypoint-initdb.d` e não tem `flyway_schema_history`; o Flyway recusaria um schema não vazio. Recriar apaga os dados locais (só dados de teste).

Run: `docker compose down -v && docker compose up -d --wait`
Expected: container `taskapi-db` healthy, banco `taskapi` vazio.

- [ ] **Step 5: Rodar todos os testes**

Run: `./mvnw test`
Expected: PASS (`contextLoads` e `v1IsApplied`). No log: `Successfully applied 1 migration to schema "public", now at version v1`. Se o Flyway avisar que o PostgreSQL 18 é mais novo que o testado, é aviso, não erro.

- [ ] **Step 6: Commit (após ok do Thiago)**

```bash
git add pom.xml docker-compose.yml src/main/resources/db src/test/java/com/thiago/taskapi/task_api/FlywayMigrationTest.java README.md taskapi.md
git commit -m "feat: migra o schema para Flyway"
```

---

### Task 2: Teto diário de cadastros

**Files:**
- Modify: `src/main/java/com/thiago/taskapi/task_api/repository/UserRepository.java`
- Create: `src/main/java/com/thiago/taskapi/task_api/exception/SignupLimitReachedException.java`
- Modify: `src/main/java/com/thiago/taskapi/task_api/service/UserService.java`
- Modify: `src/main/java/com/thiago/taskapi/task_api/exception/GlobalExceptionHandler.java`
- Modify: `src/main/java/com/thiago/taskapi/task_api/controller/UserController.java` (`@ApiResponse` 429 no `create`)
- Modify: `src/main/resources/applicationExemple.yml`
- Test: `src/test/java/com/thiago/taskapi/task_api/service/UserServiceTest.java` (Mockito, sem Spring)
- Test: `src/test/java/com/thiago/taskapi/task_api/repository/UserRepositoryTest.java` (`@SpringBootTest @Transactional`)
- Test: `src/test/java/com/thiago/taskapi/task_api/controller/UserControllerTest.java` (`@SpringBootTest @AutoConfigureMockMvc @Transactional`, `@TestPropertySource(properties = "demo.signup.daily-limit=0")`)

**Interfaces:**
- Produces:
  - `long UserRepository.countCreatedToday()` — native query: usuários com `created_at >=` meia-noite de hoje em `America/Sao_Paulo`, calculada no PostgreSQL (`date_trunc('day', now() AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'America/Sao_Paulo'`).
  - `SignupLimitReachedException extends RuntimeException` com `public static final String MESSAGE = "Limite de usuários que testaram o sistema hoje atingido";`
  - Resposta HTTP: `429`, corpo `ErrorResponse` com `message` = `SignupLimitReachedException.MESSAGE`, header `Retry-After` = segundos até a próxima meia-noite de São Paulo. **O front (Parte 2) identifica o bloqueio diário por esta mensagem em `POST /users`.**
  - Propriedade `demo.signup.daily-limit` = `${DEMO_SIGNUP_DAILY_LIMIT:100}`.
  - Construtor `UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, @Value("${demo.signup.daily-limit}") int dailySignupLimit)`.

- [ ] **Step 1: Testes que falham**

`UserServiceTest`:
- `create_throwsWhenDailyLimitReached`: `countCreatedToday()` devolve `100`, limite `100` → `assertThrows(SignupLimitReachedException.class, ...)` e `verify(userRepository, never()).save(any())`.
- `create_savesWhenBelowDailyLimit`: `countCreatedToday()` devolve `99` → `save` chamado uma vez.

`UserRepositoryTest`:
- `countCreatedToday_countsNewUser`: guarda `before = countCreatedToday()`, salva um `User`, `flush`, espera `before + 1`.
- `countCreatedToday_ignoresYesterdayInSaoPaulo`: salva dois usuários; com `JdbcTemplate` define `created_at` de um para 23:59 de ontem e do outro para 00:01 de hoje, ambos no horário de SP convertidos para o fuso padrão da JVM (`ZonedDateTime.of(..., ZoneId.of("America/Sao_Paulo")).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()` — o `created_at` é `TIMESTAMP` gravado no fuso da sessão, que o driver alinha à JVM). Espera que só o de 00:01 entre na contagem (comparar com contagem antes dos inserts).

`UserControllerTest`:
- `create_returns429WithDailyLimitMessage`: `POST /users` com corpo válido → status `429`, `$.message` igual a `SignupLimitReachedException.MESSAGE`, header `Retry-After` presente e `> 0`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw test -Dtest='UserServiceTest,UserRepositoryTest,UserControllerTest'`
Expected: FAIL de compilação (`countCreatedToday`, `SignupLimitReachedException` não existem).

- [ ] **Step 3: Implementar**

`countCreatedToday()` como `@Query(nativeQuery = true)` com a expressão das Interfaces. No `UserService.create`, checar o teto **antes** do `existsByEmail`. No `GlobalExceptionHandler`, `@ExceptionHandler(SignupLimitReachedException.class)` → 429 no formato dos outros handlers, com `Retry-After` calculado com `ZoneId.of("America/Sao_Paulo")`. Propriedade no `applicationExemple.yml` (e no `application.yml` local) sob `demo.signup.daily-limit`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw test`
Expected: PASS em todos.

- [ ] **Step 5: Conferir no Swagger**

Subir a API e abrir `/v3/api-docs`: `POST /users` tem resposta `429` com `ErrorResponse`.

- [ ] **Step 6: Commit (após ok do Thiago)**

```bash
git commit -m "feat: limita cadastros por dia na demo"
```

---

### Task 3: Limite de requisições por IP e forwarded headers

**Files:**
- Create: `src/main/java/com/thiago/taskapi/task_api/security/RateLimitFilter.java`
- Modify: `src/main/java/com/thiago/taskapi/task_api/config/SecurityConfig.java`
- Modify: `src/main/java/com/thiago/taskapi/task_api/config/OpenApiConfig.java` (429 documentado em todas as operações, no mesmo customizer do 401)
- Modify: `src/main/resources/applicationExemple.yml`
- Test: `src/test/java/com/thiago/taskapi/task_api/security/RateLimitFilterTest.java` (unitário, `MockHttpServletRequest`/`Response`, `Clock` fixo)
- Test: `src/test/java/com/thiago/taskapi/task_api/security/ForwardedHeadersTest.java` (`@SpringBootTest @AutoConfigureMockMvc`)

**Interfaces:**
- Consumes: `ErrorResponse`; `tools.jackson.databind.json.JsonMapper` (o mesmo usado pelo `RestAuthenticationHandler`).
- Produces:
  - `RateLimitFilter extends OncePerRequestFilter`, construtor `RateLimitFilter(int requestsPerMinute, Clock clock, JsonMapper jsonMapper)`.
  - Janela fixa por minuto em memória: chave = `request.getRemoteAddr()`; ao virar o minuto, o mapa é trocado por um novo (sem crescer sem limite).
  - Excedeu → `429`, `ErrorResponse` com `message` = `"Muitas requisições. Aguarde um minuto e tente novamente."`, header `Retry-After` = segundos até o fim do minuto atual.
  - `shouldNotFilter` devolve `true` para `/swagger-ui/**` e para `DispatcherType.ERROR`.
  - Propriedade `demo.rate-limit.requests-per-minute` = `${DEMO_RATE_LIMIT_PER_MINUTE:120}`.
  - `server.forward-headers-strategy: framework` no yml: o Spring passa a usar `X-Forwarded-For` como `remoteAddr` e `X-Forwarded-Prefix` nas URLs geradas (o Swagger aponta para `/api`). Seguro porque a porta da API não é exposta na VPS e o Caddy descarta `X-Forwarded-*` vindos do cliente (Parte 3 garante isso).
- O filtro é instanciado dentro do `securityFilterChain` (não como `@Component`, para não ser registrado duas vezes) e entra **antes** do `JwtAuthenticationFilter`.

- [ ] **Step 1: Testes que falham**

`RateLimitFilterTest` (limite `2`, `Clock` fixo em `2026-10-28T12:00:10Z`):
- `requestsWithinLimit_passThrough`: 2 requisições do IP `1.1.1.1` → `FilterChain` chamado 2 vezes, status `200`.
- `exceedingLimit_returns429ErrorResponse`: 3ª requisição → status `429`, corpo JSON com `"status":429` e a mensagem acima, `Retry-After` = `50`.
- `limitIsPerIp`: 2 de `1.1.1.1` e 1 de `2.2.2.2` → a de `2.2.2.2` passa.
- `newMinute_resetsCounter`: 3ª requisição com o `Clock` avançado para `12:01:00Z` passa.
- `swaggerStaticAssets_areNotLimited`: 5 requisições a `/swagger-ui/index.html` do mesmo IP → todas passam.

`ForwardedHeadersTest`:
- `apiDocsUseForwardedPrefix`: `GET /v3/api-docs` com `X-Forwarded-Prefix: /api` → `$.servers[0].url` termina em `/api`.
- `differentForwardedClients_haveSeparateLimits`: com `@TestPropertySource(properties = "demo.rate-limit.requests-per-minute=2")`, 2 requisições `POST /auth/login` (corpo inválido serve; o que importa é não ser 429) com `X-Forwarded-For: 10.0.0.1` e a 3ª com `X-Forwarded-For: 10.0.0.2` → a 3ª **não** é `429`; uma 3ª com `10.0.0.1` é `429`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw test -Dtest='RateLimitFilterTest,ForwardedHeadersTest'`
Expected: FAIL (`RateLimitFilter` não existe; servers sem `/api`).

- [ ] **Step 3: Implementar**

Filtro conforme as Interfaces (`ConcurrentHashMap<String, AtomicInteger>` + minuto corrente em campo `volatile`). No `SecurityConfig`, receber `@Value("${demo.rate-limit.requests-per-minute}") int` e `JsonMapper` no método do `securityFilterChain` e usar `Clock.systemUTC()`. Propriedades no `applicationExemple.yml` e no `application.yml` local. No `OpenApiConfig`, o customizer existente também adiciona `429` ("Limite de requisições excedido") com `ErrorResponse` a toda operação.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw test`
Expected: PASS em todos (os testes das tasks anteriores continuam passando com o limite padrão de 120).

- [ ] **Step 5: Commit (após ok do Thiago)**

```bash
git commit -m "feat: limita requisições por IP e respeita headers do proxy"
```

---

### Task 4: Limpeza automática das contas demo

**Files:**
- Modify: `src/main/java/com/thiago/taskapi/task_api/repository/UserRepository.java`
- Create: `src/main/java/com/thiago/taskapi/task_api/service/DemoAccountCleanup.java`
- Modify: `src/main/java/com/thiago/taskapi/task_api/TaskApiApplication.java` (`@EnableScheduling`)
- Test: `src/test/java/com/thiago/taskapi/task_api/repository/UserRepositoryTest.java` (mesmo arquivo da Task 2)

**Interfaces:**
- Produces:
  - `int UserRepository.deleteOlderThan24Hours()` — `@Modifying @Query(nativeQuery = true)`: `DELETE FROM users WHERE created_at < now() - interval '24 hours'`. O `ON DELETE CASCADE` do schema remove tarefas, categorias, tags e `task_tags`.
  - `DemoAccountCleanup` (`@Component`) com `@Scheduled(cron = "0 0 * * * *")` `@Transactional` `void deleteExpiredAccounts()`: chama o repositório e registra no log a quantidade apagada (`log.info`), só quando for maior que zero.

- [ ] **Step 1: Teste que falha**

`deleteOlderThan24Hours_removesOnlyOldUsersAndCascades`: salva usuário A e usuário B, uma `Task` de A; com `JdbcTemplate`, `UPDATE users SET created_at = now() - interval '25 hours' WHERE id = A` e `... - interval '1 hour' WHERE id = B`; chama `deleteOlderThan24Hours()`; espera retorno `>= 1`, A inexistente, B existente, `SELECT count(*) FROM tasks WHERE user_id = A` = `0`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw test -Dtest=UserRepositoryTest`
Expected: FAIL de compilação (`deleteOlderThan24Hours` não existe).

- [ ] **Step 3: Implementar** conforme as Interfaces.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw test`
Expected: PASS em todos.

- [ ] **Step 5: Commit (após ok do Thiago)**

```bash
git commit -m "feat: apaga contas demo com mais de 24 horas"
```

---

### Task 5: Fechamento da parte 1

**Files:**
- Modify: `taskapi.md` (seções 6 decisões, 7 endpoints/429, 8 fases, 9 próximo passo, 11 comandos)
- Modify: `README.md` (limites da demo, Flyway, variáveis `DEMO_SIGNUP_DAILY_LIMIT` e `DEMO_RATE_LIMIT_PER_MINUTE`)

- [ ] **Step 1: Teste manual de ponta a ponta com curl**

Subir com `DEMO_SIGNUP_DAILY_LIMIT=2` e `DEMO_RATE_LIMIT_PER_MINUTE=10`; verificar: 3º cadastro do dia → 429 com a mensagem do teto; 11ª requisição no mesmo minuto → 429 com a mensagem de espera; `/swagger-ui/index.html` abre mesmo após o limite; `/v3/api-docs` com `X-Forwarded-Prefix: /api` mostra servers com `/api`.

- [ ] **Step 2: Atualizar `taskapi.md` e `README.md`** com o que foi decidido nesta parte (incluindo `/swagger-ui/**` fora do limite e a forma de o front distinguir os dois 429).

- [ ] **Step 3: Commit (após ok do Thiago)**

```bash
git commit -m "docs: registra limites da demo e Flyway"
```
