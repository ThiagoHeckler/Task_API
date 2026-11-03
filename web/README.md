# Task API — demo (front)

Site de demonstração da Task API com a tela dividida: à esquerda um sistema de tarefas de verdade,
à direita a linha do tempo de cada chamada REST que ele fez, com request e response reais.

- O visitante ganha uma **conta temporária** (apagada em 24 h), preenchida com efeito de digitação.
- Todo formulário é preenchido sozinho e **espera o clique em Salvar**; só então a chamada sai.
- Erros aparecem como a API os devolve (400 com `fields`, 404, 409, 422, 429), no formulário e na
  linha do tempo.

Next.js 16 (App Router) + React 19 + TypeScript, Tailwind v4, TanStack Query v5, Vitest + Testing
Library.

## Como rodar

A API precisa estar no ar (na raiz do repositório):

```bash
docker compose up -d --wait   # PostgreSQL na porta 5440
./mvnw spring-boot:run        # API na porta 8081
```

E o front, nesta pasta:

```bash
npm install
npm run dev                   # http://localhost:3000
```

Verificação:

```bash
npm test             # Vitest
npm run lint
npx tsc --noEmit
npm run build        # gera out/ (site estático)
```

Para ver o bloqueio do dia e o aviso de espera, suba a API com limites baixos:
`DEMO_SIGNUP_DAILY_LIMIT=1 DEMO_RATE_LIMIT_PER_MINUTE=5 ./mvnw spring-boot:run`.

## Por que export estático

A página é toda client-side: ela só conversa com a API. Com `output: 'export'` o build gera HTML,
JS e CSS em `out/`, que o Caddy serve direto, sem processo Node em produção. O front sempre chama
`/api` na mesma origem, então não há CORS nem variável de ambiente com a URL da API.

## Proxy de desenvolvimento

Em produção, o Caddy leva `/api/*` até a API. No `npm run dev`, quem faz isso é o rewrite em
`next.config.ts`, que só existe na fase de desenvolvimento (export estático não aceita rewrites).

**Limitação conhecida no dev:** "Ver somente API" (`/api/swagger-ui/index.html`) abre em branco.
O rewrite do Next não envia `X-Forwarded-Prefix: /api`, então a API gera o endereço do
`swagger-config` sem o prefixo (`/v3/api-docs/swagger-config`) e o navegador recebe 404 do Next.
Em produção o Caddy envia o header e o Swagger carrega. No dev, use o Swagger direto na API:
`http://localhost:8081/swagger-ui.html`.

## Como está organizado

```
src/
├── app/            # layout, página, providers (QueryClient) e DemoApp (cadastro ou sistema)
├── components/     # TopBar, SplitLayout (colunas ≥ 768 px, abas no celular), linha do tempo
├── features/
│   ├── onboarding/ # cadastro descartável e tela de bloqueio do dia
│   ├── system/     # navegação lateral e peças de formulário compartilhadas
│   ├── tasks/      # tarefas, subtarefas e filtros
│   ├── categories/
│   ├── tags/
│   ├── account/    # Minha conta (editar nome, excluir conta)
│   └── presets.ts  # dados de exemplo que o preenchimento automático digita
├── hooks/          # useTypewriter
├── lib/            # cliente da API, store da linha do tempo, sessão, datas, tipos
└── test/           # setup do Vitest e helpers de teste
```

Decisões que valem lembrar:

- **Um cliente só** (`lib/api.ts`): anexa o token e registra cada requisição na linha do tempo
  antes do `fetch` e de novo com status, corpo e duração (inclusive em erro e falha de rede).
- **O token nunca aparece inteiro na tela**: no header `Authorization` e no corpo do login ele
  aparece com os 10 primeiros caracteres e `…`.
- **Nenhuma chamada automática**: o TanStack Query não refaz requisições sozinho (foco, reconexão,
  retry). Cada linha da linha do tempo vem de uma ação do visitante; depois de salvar, a lista é
  invalidada e o `GET` seguinte, real, aparece também.
- **Datas**: a API devolve UTC sem offset; o front exibe e recebe no horário de São Paulo.
- **Token em `sessionStorage`**: sobrevive a recarregar a aba e some ao fechá-la. Qualquer 401
  com token volta para o cadastro.
