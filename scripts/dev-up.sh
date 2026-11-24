#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || { echo "Create .env from .env.example first"; exit 1; }
docker compose --env-file .env -f infrastructure/docker-compose.yml up --build
