# Demo com tela dividida — design

Data: 28/10/2026 · Projeto: Task API · Abrange as fases 9 (deploy) e 11 (front), antecipadas antes da fase 10.

## 1. Intenção

A Task API é o primeiro projeto do portfólio Spring (`thiagoheckler.com.br`). Hoje, quem abre o
projeto vê só o Swagger: tecnicamente completo, mas exige que o visitante saiba usar uma API
(cadastrar, logar, copiar token, clicar em Authorize). Um recrutador não faz isso.

A demo resolve: o visitante usa um **sistema de tarefas de verdade** à esquerda e, à direita, vê
**em tempo real as chamadas que esse sistema faz na API**. Ele entende o backend sem precisar
saber usar Swagger — e quem quiser o Swagger continua tendo.

Sucesso = um recrutador abre o link, clica em poucas coisas e sai sabendo que existe uma API REST
com JWT por trás, vendo as rotas, os corpos e os status reais.

## 2. Objetivos

- Tela dividida: **esquerda = sistema simulado**, **direita = linha do tempo das chamadas**.
- Botão no topo **"Ver somente API"**: troca a tela pelo Swagger UI atual, em tela cheia (e um
  caminho de volta para a demo).
- **Cadastro descartável** como primeira tela: só existe para gerar um usuário e o token.
  O token é usado automaticamente (o visitante nunca copia token), mas a linha do tempo mostra
  onde ele é gerado (`POST /auth/login`) e onde passa a ser usado (header `Authorization: Bearer`).
- Opções do sistema: **Tarefas, Categorias, Tags e Minha conta** (o "usuários" do pedido; a API só
  expõe a própria conta, `/users/me`).
- Ao escolher uma ação (ex.: Tarefas → Nova tarefa), os campos **se preenchem sozinhos, com efeito
  de digitação**, e o sistema **para e espera o visitante clicar em Salvar**. Só então a chamada sai.
- Editar/excluir itens das listas também aparece na linha do tempo (PUT/DELETE).
- **Tudo é real**: as chamadas vão para a API e o banco de produção. Nada é encenado.
- Publicado na internet junto com o deploy da API.

## 3. Não-objetivos

- Contas permanentes: todo usuário é de demonstração e é apagado.
- Recuperação de senha, perfis, permissões, multiusuário colaborativo.
- Refresh token (decisão já tomada no projeto).
- Um front "produto" completo: o sistema existe para demonstrar a API.
- Testes automatizados amplos da API (são a fase 10); aqui só os testes do código novo.

## 4. Anti-objetivos (falha mesmo funcionando)

- A linha do tempo **mostrar algo diferente do que realmente foi enviado/recebido** (corpo
  "bonito" inventado). Ela mostra o request e a resposta reais.
- O visitante precisar saber o que é token, Swagger ou JSON para conseguir usar a demo.
- O site cair ou o banco encher por abuso (cadastro é público).
- A demo consumir RAM que a VPS não tem (ver §5).

## 5. Restrições

- **Hospedagem já decidida no portfólio:** subdomínio `tasks.thiagoheckler.com.br` na
  VPS Hostinger (KVM 1: 1 vCPU, 4 GB, compartilhada com OKM, Evolution e outros), dentro do repo
  `github.com/ThiagoHeckler/Portfolio` (`docker-compose.yml` com âncora `x-java-app` de 384 MB /
  `-Xmx160m` / SerialGC, `caddy/Caddyfile`, PostgreSQL 17 compartilhado com vários bancos,
  `postgres/init/01-databases.sql`). HTTPS automático pelo Caddy; DNS Cloudflare com `*` DNS-only.
- **Desencontros a resolver no deploy:**
  - Os outros projetos do portfólio usam **Flyway**; a Task API usa `schema.sql` + `ddl-auto: validate`
    (resolvido: adotar Flyway, ver §6).
  - Local roda PostgreSQL 18; a VPS tem PostgreSQL 17. O `schema.sql` precisa rodar no 17.
- **Segredos só em variáveis de ambiente** (`JWT_SECRET` real, credenciais do banco); manter
  `applicationExemple.yml` como referência sem segredo.
- **Não instalar dependência nova sem justificar** (regra do Thiago).
- Commits em português, Conventional Commits; nada de commit/push sem pedido.
- Thiago está aprendendo Next.js: o código do front deve ser simples e as decisões explicadas.

## 6. Decisões tomadas

Do Thiago:
- Painel direito = **linha do tempo das chamadas** (não o Swagger embutido). O Swagger fica no
  botão "Ver somente API".
- Preencher e **esperar o clique em Salvar** (não é demo autoplay).
- **Visual "B"**: sistema em tema claro (estilo SaaS, acento índigo `#4f46e5`) + painel da API
  escuro com cara de console (fundo `#0f172a`, monoespaçada, selos coloridos por método:
  POST verde, GET azul, PUT laranja, DELETE vermelho). Mockup aprovado em
  `.superpowers/brainstorm/…/content/visual-style.html`.
- **Next.js** para o front.
- Publicar junto com o deploy da API.
- **Proteção contra abuso, aprovada:**
  - limite de requisições **por IP**;
  - **teto global de cadastros por dia**, independente de IP (ataque pode trocar de IP). Ao
    atingir, `POST /users` recusa e o site **trava** com a mensagem
    *"Limite de usuários que testaram o sistema hoje atingido"*.
  - contas de demo apagadas automaticamente depois de um tempo.
- Swagger **ligado em produção** (precisa existir para "Ver somente API").
- **Flyway** para o schema, alinhado com os outros projetos do portfólio: o `schema.sql` atual
  vira `V1__schema_inicial.sql` (incluindo a trigger de subtarefa); `ddl-auto: validate` continua.
  Dependências novas: `flyway-core` e `flyway-database-postgresql`. O banco local precisa ser
  recriado (ou receber baseline) e o `docker-compose.yml` deixa de montar o `schema.sql` no
  `docker-entrypoint-initdb.d`.

Do Claude (aprovadas na conversa como pacote, revisar ao ler):
- **TypeScript** no front.
- **Next.js exportado como site estático** (`output: 'export'`), servido pelo próprio Caddy:
  zero RAM extra na VPS e sem container a mais. Não há necessidade de servidor Node: o front só
  chama a API pelo navegador.
- **Mesmo domínio**: front em `tasks.thiagoheckler.com.br/`, API em
  `tasks.thiagoheckler.com.br/api/*` (Caddy remove o prefixo `/api` e repassa para o container).
  Sem CORS. O Swagger fica em `/api/swagger-ui/index.html`.
- **Front no mesmo repositório**, pasta `web/`, para o recrutador ver tudo junto.
- Teto diário padrão: **100 cadastros/dia**, configurável por variável de ambiente.
- Contas apagadas quando têm **mais de 24 h** (o `ON DELETE CASCADE` já leva tarefas,
  categorias e tags).
- Antes de salvar, o visitante **pode alterar** os campos preenchidos; a linha do tempo mostra o
  que foi realmente enviado.
- O teto diário bloqueia **só novos cadastros**: quem já entrou continua usando a demo até o
  token expirar. Cadastros feitos pelo "Try it out" do Swagger contam no mesmo teto.
- O limite por IP vale para a API inteira (inclusive login e Swagger), não só para o cadastro.

## 7. Comportamento da demo

### 7.1 Primeira tela (cadastro descartável)
- Formulário de cadastro já preenchido com dados gerados (nome tipo "Visitante 4821", email
  único em domínio fictício, senha gerada), com o efeito de digitação.
- Texto curto explicando: "Conta temporária de demonstração, apagada em 24 h".
- Um clique em **Criar conta e entrar** dispara `POST /users` e, em seguida, `POST /auth/login`
  automaticamente. A linha do tempo destaca o token gerado na resposta do login.
- A partir daí, toda chamada mostra o header `Authorization: Bearer eyJ…` (token abreviado).

### 7.2 Sistema
- Navegação lateral: Tarefas, Categorias, Tags, Minha conta.
- Cada área: lista + ação de criar; itens da lista com editar e excluir.
- **Tarefas** cobre o que a API oferece: título, descrição, prioridade, prazo, status
  (mudar para `COMPLETED` mostra `completedAt` preenchido na resposta), categoria, tags,
  subtarefa (`parentTaskId`), filtro por status (`GET /tasks?status=`) e tarefas principais
  (`GET /tasks/root`).
- Dados de exemplo coerentes entre si (ex.: criar categoria "Estudos" e depois a tarefa usa ela),
  para os preenchimentos automáticos não darem 404 por id inexistente.
- **Minha conta**: ver, editar nome e excluir conta (`DELETE /users/me` → volta para o cadastro).

### 7.3 Linha do tempo (painel direito)
- Uma linha por chamada, a mais recente em destaque: método, rota (com `/api`), status, tempo.
- Expandir mostra request (headers relevantes + corpo) e resposta (corpo) reais.
- Erros aparecem como erros (400 com `fields`, 404, 409, 422), com o corpo real.
- Antes do clique em Salvar, um estado "aguardando você salvar…".

### 7.4 Topo
- Nome do projeto, botão **Ver somente API** (abre o Swagger em tela cheia) e link para o
  repositório no GitHub.

### 7.5 Limites e expiração
- `429` do teto diário → tela de bloqueio com a mensagem aprovada, em qualquer ponto da demo.
- `429` do limite por IP → aviso para esperar um pouco.
- Token expirado (1 h) ou conta já apagada → volta para o cadastro descartável com aviso.

## 8. Mudanças na API

- **Teto diário de cadastros** em `POST /users`: contar usuários criados no dia (fuso
  `America/Sao_Paulo`, coluna `users.created_at` já existe) e responder `429` com `ErrorResponse`
  e mensagem própria ao atingir o teto. Valor via variável de ambiente.
- **Limite por IP**: por IP real do visitante (atrás do Caddy, vem de `X-Forwarded-For`; a API
  deve confiar só no proxy). Responde `429` em `ErrorResponse`.
- **Limpeza agendada**: apaga usuários com `created_at` acima de 24 h.
- **Atrás de proxy com prefixo `/api`**: o Swagger precisa gerar URLs com `/api` (forwarded
  headers / `X-Forwarded-Prefix`).
- Documentar o novo `429` no Swagger.
- Testes para o teto diário, o limite por IP e a limpeza.

## 9. Deploy

- Imagem Docker da API (multi-stage), seguindo o padrão `x-java-app` do repo Portfolio.
- Banco `taskapi` no PostgreSQL 17 compartilhado (criação no `01-databases.sql` ou equivalente)
  e schema aplicado pelo Flyway na subida da API.
- Caddyfile: `tasks.thiagoheckler.com.br` servindo o `web/out` estático em `/` e
  `/api/*` → container da API sem o prefixo.
- `JWT_SECRET`, credenciais e teto diário via env, documentados no `.env.example` do Portfolio.
- Atualizar README (link da demo), `taskapi.md` e a nota do vault.

## 10. Deixado para quem for construir

- Como implementar o limite por IP (filtro próprio em memória vs. biblioteca); qualquer
  dependência nova precisa de justificativa.
- Valor do limite por IP.
- Bibliotecas do front (estilização, estado, requisições), mantendo o mínimo.
- Layout em celular (a tela dividida não cabe: provavelmente abas Sistema / API).
- Velocidade e detalhes do efeito de digitação.
- Onde guardar o token no navegador (memória vs. `sessionStorage`).
