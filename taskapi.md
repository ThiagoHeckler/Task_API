# Task API — contexto do projeto

Documento de passagem de contexto para continuar o desenvolvimento no Claude Code.
Estado em 09/10/2026 (atualizado ao fim da fase 7).

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
| Jackson | 3 (pacote `tools.jackson`; o bean é `JsonMapper`) |
| PostgreSQL | 18 (container Docker) |
| Maven | 3.9.16 (wrapper `./mvnw` versionado em `.mvn/`) |
| JWT | JJWT 0.12.6 (`jjwt-api` compile; `jjwt-impl` e `jjwt-jackson` runtime) |

- **Porta da aplicação: 8081** (a 8080 está ocupada por um container Docker).
- **Banco via `docker-compose.yml`**: container `taskapi-db`, imagem `postgres:18-alpine`,
  porta **5440** no host (5432–5434 estão ocupadas por outros projetos). Banco `taskapi`,
  usuário/senha `thiago`/`thiago` por padrão (`DB_USER` / `DB_PASSWORD`).
- **Uma máquina só**: o desenvolvimento continua apenas na máquina com Arch Linux. Os clones
  antigos (Zorin, Elementary) foram abandonados e têm histórico incompatível (ver seção 10).
- `application.yml` está no `.gitignore`. O modelo versionado é
  `src/main/resources/applicationExemple.yml` — mudanças de configuração vão nos dois.

---

## 4. Estrutura

```
src/main/java/com/thiago/taskapi/task_api/
├── TaskApiApplication.java   # exclui UserDetailsServiceAutoConfiguration
├── config/      SecurityConfig
├── security/    JwtService, JwtAuthenticationFilter, RestAuthenticationHandler
├── controller/  AuthController, UserController, CategoryController, TagController, TaskController
├── dto/         records de request/response + ErrorResponse, ValidationErrorResponse
├── exception/   ResourceNotFoundException, DuplicateResourceException,
│                InvalidCredentialsException, GlobalExceptionHandler
├── model/       User, Category, Tag, Task
│   └── enums/   TaskStatus (PENDING, IN_PROGRESS, COMPLETED), TaskPriority (LOW, MEDIUM, HIGH)
├── repository/  UserRepository, CategoryRepository, TagRepository, TaskRepository
└── service/     AuthService, UserService, CategoryService, TagService, TaskService
src/main/resources/
├── schema.sql              # aplicado pelo docker-compose na primeira subida
└── applicationExemple.yml  # modelo de configuração
docker-compose.yml          # PostgreSQL 18 na porta 5440
```

Pacote base: `com.thiago.taskapi.task_api` (o Initializr não aceitou `task-api` com hífen).

---

## 5. Banco de dados

O **banco é a fonte da verdade**: `ddl-auto: validate`. O `schema.sql` é montado em
`/docker-entrypoint-initdb.d/` e roda **só quando o volume está vazio** — mudou o schema,
precisa recriar o volume (`docker compose down -v && docker compose up -d`).

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
- **JJWT escolhido em vez de Nimbus** pelo valor didático.
- **JWT:** segredo em `jwt.secret` (`${JWT_SECRET:fallback}`), expiração `jwt.expiration`
  = 1 h; subject = `userId`; chave criada uma vez com `Keys.hmacShaKeyFor()` (lança
  `WeakKeyException` com menos de 32 bytes — a app nem sobe). Com o segredo atual o
  algoritmo sai HS512. **Sem refresh token** (limitação consciente).
- **Login (`AuthService`):** e-mail inexistente e senha errada dão o mesmo 401
  "Credenciais inválidas", **inclusive no tempo** — sem usuário, o BCrypt roda contra um
  hash falso gerado no construtor.
- **Filtro (`JwtAuthenticationFilter`) não é `@Component`:** o Boot registraria o bean
  também como filtro de servlet e ele rodaria duas vezes. É instanciado no `SecurityConfig`.
  Token ausente/inválido → segue sem autenticação; o 401 sai do entry point. O filtro não
  consulta o banco: token de usuário excluído vale até expirar (operações dão 404).
- **`SecurityConfig`:** `STATELESS`; públicos só `POST /auth/login` e `POST /users`;
  `DispatcherType.ERROR` liberado (senão o despacho para `/error` perde a autenticação e
  todo 404/500 vira 401).
- **401/403 via `RestAuthenticationHandler`** (entry point + access denied handler),
  escrevendo `ErrorResponse` com o `JsonMapper`. O `@RestControllerAdvice` não alcança
  exceções lançadas nos filtros.
- **Usuário vem do token:** controllers recebem `@AuthenticationPrincipal Long userId`;
  não existe `{userId}` em nenhuma URL. Usuário só opera a própria conta (`/users/me`);
  `GET /users` e `/users/{id}` foram removidos (não há papel de admin).
- **Recurso de outro usuário → 404, não 403** (não confirmar que o id existe).
- **Stack trace nunca vai na resposta:** `spring.web.error.include-stacktrace: never`.
  No Boot 4 a propriedade antiga `server.error.*` é ignorada.

---

## 7. Endpoints atuais

Todos funcionais e testados via curl. Exceto os marcados como públicos, exigem
`Authorization: Bearer <token>`.

| Recurso | Rotas |
|---|---|
| Auth | `POST /auth/login` (público) → `{token, type: "Bearer", expiresIn}` |
| Usuários | `POST /users` (público), `GET/PUT/DELETE /users/me` |
| Categorias | `POST/GET /categories`, `GET/PUT/DELETE /categories/{id}` |
| Tags | `POST/GET /tags`, `GET/PUT/DELETE /tags/{id}` |
| Tarefas | `POST/GET /tasks` (`?status=` opcional), `GET /tasks/root`, `GET/PUT/DELETE /tasks/{id}` |

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
| 7 | Autenticação JWT | ✅ |
| 8 | Swagger / OpenAPI | ⬜ **próxima** |
| 9 | Deploy | ⬜ |
| 10 | Fechamento da API (testes, polimento) | ⬜ |
| 11 | Front-end de vitrine (opcional; decidir depois da fase 10) | ⬜ |

README e este documento usam a mesma numeração.

---

## 9. Próximo passo — fase 8 (Swagger)

Motivação imediata: com JWT, a API não é testável pela barra do navegador (sem header
`Authorization`). O Swagger UI resolve isso com "Try it out" + botão "Authorize".

Escopo previsto:
1. Dependência `springdoc-openapi` compatível com Spring Boot 4.
2. Configuração do esquema de segurança Bearer (JWT) no OpenAPI.
3. Liberar `/swagger-ui/**` e `/v3/api-docs/**` no `SecurityConfig`.
4. Decidir depois: anotações de descrição nos endpoints/DTOs e se o Swagger fica ativo
   em produção.

---

## 10. Pendências e pontos de atenção encontrados no código

Resolvido nesta rodada:
- Arquivo `exit` (vazava e-mail e `password_hash`) removido **reescrevendo o histórico**
  (force push em 09/10/2026). O GitHub pode manter o commit antigo `37b3cd4` acessível por
  hash por um tempo; para remoção definitiva, pedir ao suporte do GitHub. Clones antigos
  não devem dar `git pull` (traria o arquivo de volta).
- README atualizado (rotas, schema, execução com Docker, numeração das fases).
- Typo `secrect` no yml; imports sem uso no `TagController`; barra no `@DeleteMapping`
  do `TaskController`.

Segurança / repositório:
- README cita licença MIT e um arquivo `LICENSE`, mas **o arquivo não existe**. Criar.
- Push pendente: os commits da fase 7 estão só locais até o próximo `git push`.

Bugs / comportamento:
- Criar subtarefa de uma subtarefa dispara a trigger do banco, mas a exceção não é tratada →
  hoje vira **500**. Opções: validar no `TaskService.create` (pai precisa ter
  `parentTask == null`) e/ou tratar `DataIntegrityViolationException` no handler.
- `UpdateTaskRequest.title` tem `@Size(max = 50, message = "O nome ...")`, mas no create e no
  banco o limite é 255. Alinhar para 255 e corrigir a mensagem.
- Erros que caem no `/error` padrão do Spring (ex.: 405) saem num formato diferente do
  `ErrorResponse` (têm `path`). Padronizar, p. ex. estendendo `ResponseEntityExceptionHandler`.
- Não há handler genérico para `Exception` → um erro inesperado sai no formato padrão do
  Spring (já sem stack trace).

Limpeza:
- `TagService` ainda importa `CategoryResponse`, `UpdateCategoryRequest` e `Category` sem usar.
- `UserService`, `CategoryService` e `TagService` não têm `@Transactional` (o `TaskService` e
  o `AuthService` têm). Padronizar.
- Typos em mensagens: `"ecnontrado"` (TaskService.create), `"catacteres"` (UpdateTagRequest),
  `"unknow"` (GlobalExceptionHandler).
- Comentário solto no fim do `TagRepository` (`// ver sobre ...bootstrap-mode`).
- `TaskRepository.findByUserIdAndPriority` existe mas não é usado (dá para expor `?priority=`).
- `toResponse` de `TaskService` acessa categoria e tags lazy → N+1 nas listagens. Avaliar
  `@EntityGraph` / `JOIN FETCH` mais adiante.
- Ordem dos campos no `ValidationErrorResponse` segue a ordem do Spring, não a do record.

Adiado de propósito:
- `Instant` vs `LocalDateTime` nos timestamps das entidades (os DTOs de erro já usam `Instant`).
- Testes automatizados: hoje só existe `contextLoads`.

---

## 11. Comandos úteis

```bash
# subir o banco e a aplicação
docker compose up -d
./mvnw spring-boot:run

# recriar o banco do zero (CUIDADO: apaga dados; reaplica o schema.sql)
docker compose down -v && docker compose up -d

# psql dentro do container
docker exec -it taskapi-db psql -U thiago -d taskapi

# criar usuário e fazer login
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Thiago","email":"thiago@teste.com","password":"senhaSegura123"}'

TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"thiago@teste.com","password":"senhaSegura123"}' | jq -r .token)

# criar tarefa autenticado
curl -X POST http://localhost:8081/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Estudar JWT","priority":"HIGH","tagIds":[1]}'
```
