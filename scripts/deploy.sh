#!/usr/bin/env bash
# Publica a demo em tasks.thiagoheckler.com.br.
#
# Uso (da raiz do repo):
#   VPS=dev@72.60.49.243 PORTFOLIO_DIR=/caminho/do/Portfolio/na/vps scripts/deploy.sh
#
# Pré-requisitos: `docker login` feito, ./mvnw test verde, VPS preparada uma vez
# (README do repo Portfolio, seção "Deploy da Task API").
#
# Rollback da API (na VPS, dentro do PORTFOLIO_DIR):
#   docker pull thiagoheckler/task-api:<sha-anterior>
#   docker tag thiagoheckler/task-api:<sha-anterior> thiagoheckler/task-api:latest
#   docker compose up -d task-api
# Só é seguro se não houver migration nova do Flyway entre as duas versões.
# O front volta rodando este script a partir do commit anterior.
set -euo pipefail

: "${VPS:?defina VPS, ex.: dev@72.60.49.243}"
: "${PORTFOLIO_DIR:?defina PORTFOLIO_DIR, a pasta do repo Portfolio na VPS}"

cd "$(git rev-parse --show-toplevel)"

# A imagem e o site saem da árvore de trabalho: só publica o que está commitado.
if [ -n "$(git status --porcelain -- pom.xml src web)" ]; then
  echo "Há mudanças não commitadas em pom.xml, src/ ou web/. Commit antes de publicar." >&2
  exit 1
fi

IMAGE=thiagoheckler/task-api
SHA=$(git rev-parse --short HEAD)

echo "==> Imagem $IMAGE:$SHA"
docker build -t "$IMAGE:latest" -t "$IMAGE:$SHA" .
docker push "$IMAGE:$SHA"
docker push "$IMAGE:latest"

echo "==> Front (web/out)"
npm --prefix web ci
npm --prefix web run build
rsync -a --delete web/out/ "$VPS:$PORTFOLIO_DIR/sites/tasks/"

echo "==> API na VPS"
ssh "$VPS" "cd '$PORTFOLIO_DIR' && docker compose pull task-api && docker compose up -d task-api"

echo "Publicado: $IMAGE:$SHA"
