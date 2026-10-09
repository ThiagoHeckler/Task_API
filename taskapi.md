# Task API — contexto do projeto

Documento de passagem de contexto para continuar o desenvolvimento no Claude Code.
Estado em 09/10/2026.

---

## 1. O que é

API REST de gerenciamento de tarefas (to-do list), projeto de portfólio solo do Thiago.
Objetivo: mostrar em processos seletivos domínio de modelagem relacional, JPA/Hibernate,
validação, tratamento de erros e autenticação stateless com JWT.

Artefatos de portfólio planejados: README forte, Swagger/OpenAPI, deploy público.

---

## 2. Como trabalhamos (importante para o Claude Code)

- **Desenvolvimento incremental e explicado.** Cada decisão é discutida e validada antes de
  escrever código. Não gerar fases inteiras de uma vez; propor, explicar o porquê, esperar o ok.
- **Testes com estado de banco conhecido.** Antes de concluir que há bug, garantir que o banco
  está num estado conhecido (dados podem mudar entre execuções e gerar falsos positivos).
- **Testes manuais via curl** até a fase de testes automatizados.
- IDE principal: Spring Tool Suite. Se aparecer comportamento estranho de `.class` antigo:
  `Project → Clean` e rodar de novo.

---

## 3. Stack e ambiente

| Item | Versão / detalhe |
|---|---|
| Java | 25 (Temurin via SDKMAN) |
| Spring Boot | 4.1.0 (`spring-boot-starter-webmvc`, data-jpa, validation, security) |
| Hibernate | 7.4.1 |
| PostgreSQL | 18 |
| Maven | 3.9.16 (wrapper `./mvnw` versionado em `.mvn/`) |
| JWT | JJWT 0.12.6 (`jjwt-api` compile; `jjwt-impl` e `jjwt-jackson` runtime) |

- **Porta da aplicação: 8081** (a 8080 está ocupada por um container Docker).
- Banco: `taskapi`, role `thiago`.
- Duas máquinas sincronizadas pelo GitHub:
  - Desktop Zorin OS → PostgreSQL nativo
  - Notebook Elementary OS → PostgreSQL via Docker Compose
- `application.yml` está no `.gitignore`. O modelo versionado é
  `src/main/resources/applicationExemple.yml`.

---

## 4. Estrutura

```
src/main/java/com/thiago/taskapi/task_api/
├── TaskApiApplication.java   # exclui UserDetailsServiceAutoConfiguration
├── config/      SecurityConfig
├── controller/  UserController, CategoryController, TagController, TaskController
├── dto/         records de request/response + ErrorResponse, ValidationErrorResponse
├── exception/   ResourceNotFoundException, DuplicateResourceException, GlobalExceptionHandler
├── model/       User, Category, Tag, Task
│   └── enums/   TaskStatus (PENDING, IN_PROGRESS, COMPLETED), TaskPriority (LOW, MEDIUM, HIGH)
├── repository/  UserRepository, CategoryRepository, TagRepository, TaskRepository
└── service/     UserService, CategoryService, TagService, TaskService
src/main/resources/
├── schema.sql              # schema aplicado manualmente
└── applicationExemple.yml  # modelo de configuração
```

Pacote base: `com.thiago.taskapi.task_api` (o Initializr não aceitou `task-api` com hífen).

---

## 5. Banco de dados

O **banco é a fonte da verdade**: `ddl-auto: validate`, schema aplicado à mão com
`psql -d taskapi -f src/main/resources/schema.sql`.

Tabelas: `users`, `categories`, `tags`, `tasks`, `task_tags` (pivô N:N).

Pontos de design do schema:
- ENUMs nativos do PostgreSQL: `task_status`, `task_priority`. Mapeados no JPA com
  `@Enumerated(STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` + `columnDefinition`.
- Categorias e tags têm escopo por usuário: `UNIQUE (user_id, name)`.
- FKs: `tasks.user_id` e `parent_task_id` com `ON DELETE CASCADE`; `category_id` com
  `ON DELETE SET NULL`; `task_tags` cascateia nos dois lados.
- Índices compostos `(user_id, status)` e `(user_id, priority)`, e parciais para
  `parent_task_id`, `due_date` e `category_id`.
- **Trigger `trg_tasks_one_level`**: subtarefa só pode ter um nível. Virou trigger porque o
  PostgreSQL não permite subquery em `CHECK`.
- **Trigger `trg_tasks_updated_at`**: atualiza `updated_at` em todo UPDATE.
- `created_at` / `updated_at` lidos de volta pelo Hibernate com
  `@Generated(event = ...)` + `insertable = false, updatable = false`.

---

## 6. Decisões técnicas (já tomadas — não rediscutir sem motivo)

- **Sem Lombok nas entidades.** Getters/setters à mão; evita `equals`/`hashCode`/`toString`
  percorrendo relacionamentos e disparando lazy loading. DTOs são `record`.
- **IDs como `Long`, nunca `long`.** Hibernate precisa de `null` para distinguir entidade
  transiente de persistida. Comparar com `.equals()`.
- **PUT como atualização parcial pragmática.** Só campos não nulos são aplicados. Não há PATCH.
  Consequência conhecida: não dá para "limpar" um campo opcional (ex.: remover categoria ou
  prazo) enviando `null`.
- **Regra de `completedAt`** em `TaskService.update`: vira `now()` quando o status passa para
  `COMPLETED` (se ainda estiver nulo); volta a `null` quando sai de `COMPLETED`.
- **`parentTaskId` não é editável** no update (só definido na criação).
- **Título de tarefa pode repetir** (sem checagem de duplicidade, de propósito).
- **Padrão de checagem de duplicidade no update:**
  `!novo.equals(atual) && existsBy...` — evita conflito falso ao reenviar o mesmo valor.
- **Transações em `TaskService`:** `@Transactional(readOnly = true)` na classe,
  `@Transactional` nos métodos de escrita.
- **Erros padronizados** via `@RestControllerAdvice`:
  - 404 `ResourceNotFoundException`, 409 `DuplicateResourceException`
  - 400 validação (`ValidationErrorResponse` com mapa `campo → mensagem`, `LinkedHashMap`
    para ordem estável)
  - 400 JSON malformado — mensagem genérica, **sem vazar `ex.getMessage()`**
  - 400 tipo de parâmetro errado (`getRequiredType()` tratado como possivelmente nulo)
  - Status e textos vêm de `HttpStatus` (`value()` / `getReasonPhrase()`), timestamps em `Instant`.
- **`@Valid` vai no `@RequestBody`**, não no `@PathVariable`.
- **Spring Security entrou cedo só pelo BCrypt.** `SecurityConfig` com `permitAll`, CSRF,
  `formLogin` e `httpBasic` desabilitados; `UserDetailsServiceAutoConfiguration` excluída
  na classe principal.
- **JJWT escolhido em vez de Nimbus** pelo valor didático.
- **Rotas provisoriamente escopadas por usuário:** `/users/{userId}/...`. Com o JWT pronto,
  o usuário autenticado deve vir do token e o `{userId}` sai da URL.

---

## 7. Endpoints atuais

Todos funcionais e testados via curl.

| Recurso | Rotas |
|---|---|
| Usuários | `POST /users`, `GET /users`, `GET /users/{id}`, `PUT /users/{id}`, `DELETE /users/{id}` |
| Categorias | `POST/GET /users/{userId}/categories`, `GET/PUT/DELETE /users/{userId}/categories/{id}` |
| Tags | `POST/GET /users/{userId}/tags`, `GET/PUT/DELETE /users/{userId}/tags/{id}` |
| Tarefas | `POST/GET /users/{userId}/tasks` (`?status=` opcional), `GET /users/{userId}/tasks/root`, `GET/PUT/DELETE /users/{userId}/tasks/{id}` |

---

## 8. Fases

| # | Fase | Status |
|---|---|---|
| 1 | Setup | ✅ |
| 2 | Modelagem do banco | ✅ |
| 3 | Entidades JPA | ✅ |
| 4 | Repositories | ✅ |
| 5 | DTOs | ✅ |
| 6 | Services + Controllers (CRUD completo das 4 entidades) | ✅ |
| 7 | Autenticação JWT | 🚧 **em andamento** |
| 8 | Swagger / OpenAPI | ⬜ |
| 9 | Deploy | ⬜ |
| 10 | Fechamento da API (testes, polimento) | ⬜ |
| 11 | Front-end de vitrine (opcional; decidir depois da fase 10) | ⬜ |

> O README usa outra numeração (JWT aparece como fase 9). Alinhar quando for atualizar o README.

---

## 9. Fase 7 — JWT: onde paramos

Já decidido:
- Segredo via `${JWT_SECRET:fallback}` no `application.yml`; expiração de 1 hora
  (`3600000` ms); **sem refresh token** (limitação consciente).
- Subject do token = `userId` (`Long`).
- Chave criada uma vez no construtor com `Keys.hmacShaKeyFor()`.
- API do JJWT 0.12.x: `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`.
  (`parseClaimsJws` / `setSigningKey` estão depreciados.)
- `isTokenValid` captura `JwtException` e `IllegalArgumentException`.

Situação: um `JwtService` foi escrito, mas a sessão terminou antes de testá-lo.
**Ele não está entre os arquivos atuais do repositório** — verificar se ficou sem commit em
alguma das máquinas antes de reescrever.

Próximos passos sugeridos para a fase:
1. Corrigir `secrect` → `secret` em `applicationExemple.yml` (e no `application.yml` local).
   Esse typo é a causa provável de falha ao subir a app com `@Value("${jwt.secret}")`.
2. Recuperar/validar o `JwtService` (gerar token, extrair `userId`, validar).
3. DTOs `LoginRequest` / `LoginResponse` e `AuthController` com `POST /auth/login`
   (busca por e-mail, `passwordEncoder.matches`, devolve token).
4. Filtro `OncePerRequestFilter` que lê `Authorization: Bearer ...` e popula o
   `SecurityContext`.
5. `SecurityConfig`: sessão `STATELESS`, liberar `POST /auth/login` e `POST /users`,
   exigir autenticação no resto; respostas 401/403 no mesmo formato de `ErrorResponse`.
6. Tirar `{userId}` das rotas e usar o usuário do token; restringir `GET /users`
   (hoje lista todos os usuários sem autenticação).

---

## 10. Pendências e pontos de atenção encontrados no código

Segurança / repositório:
- **Arquivo `exit` na raiz do projeto** contém a saída de um `SELECT` com e-mail e
  `password_hash` de usuário. Parece ter sido criado sem querer no `psql`. Remover e garantir
  que não fique no histórico do Git.

Bugs / comportamento:
- Criar subtarefa de uma subtarefa dispara a trigger do banco, mas a exceção não é tratada →
  hoje vira **500**. Opções: validar no `TaskService.create` (pai precisa ter
  `parentTask == null`) e/ou tratar `DataIntegrityViolationException` no handler.
- `UpdateTaskRequest.title` tem `@Size(max = 50, message = "O nome ...")`, mas no create e no
  banco o limite é 255. Alinhar para 255 e corrigir a mensagem.

Limpeza:
- `TagService` e `TagController` importam `CategoryResponse`, `UpdateCategoryRequest` e
  `Category` sem usar.
- `UserService`, `CategoryService` e `TagService` não têm `@Transactional` (o `TaskService` tem).
  Padronizar.
- Typos em mensagens: `"ecnontrado"` (TaskService.create), `"catacteres"` (UpdateTagRequest),
  `"unknow"` (GlobalExceptionHandler).
- `@DeleteMapping("{id}")` no `TaskController` sem a barra inicial — funciona, mas destoa dos
  outros.
- Comentário solto no fim do `TagRepository` (`// ver sobre ...bootstrap-mode`).
- `TaskRepository.findByUserIdAndPriority` existe mas não é usado (dá para expor `?priority=`).
- `toResponse` de `TaskService` acessa categoria e tags lazy → N+1 nas listagens. Avaliar
  `@EntityGraph` / `JOIN FETCH` mais adiante.

README desatualizado:
- Rotas listadas como `/categories`, `/tags`, `/tasks` (o real é `/users/{userId}/...`) e
  marcadas como 🚧, mas já funcionam.
- Caminho do schema aparece como `src/main/resources/db/schema.sql` (real:
  `src/main/resources/schema.sql`) e configuração em `application.properties` (real: `.yml`).
- Diagrama ER mostra `users.updated_at`, `timestamptz` e `due_date date`, que não batem com o
  `schema.sql` (`TIMESTAMP`, sem `updated_at` em users).

Adiado de propósito:
- `Instant` vs `LocalDateTime` nos timestamps das entidades (os DTOs de erro já usam `Instant`).
- Testes automatizados: hoje só existe `contextLoads`.

---

## 11. Comandos úteis

```bash
# subir a aplicação
./mvnw spring-boot:run

# recriar o schema do zero (CUIDADO: apaga dados)
dropdb taskapi && createdb taskapi
psql -d taskapi -f src/main/resources/schema.sql

# exemplo: criar usuário
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Thiago","email":"thiago@teste.com","password":"senhaSegura123"}'

# exemplo: criar tarefa
curl -X POST http://localhost:8081/users/1/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Estudar JWT","priority":"HIGH","tagIds":[1]}'
```
