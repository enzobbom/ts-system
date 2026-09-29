#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

COMPOSE=(
  docker compose
  --env-file "$ROOT_DIR/.env.e2e"
  -f "$ROOT_DIR/docker-compose.yml"
  -f "$ROOT_DIR/docker-compose.e2e.yml"
)

cleanup() {
  exit_code=$?

  trap - EXIT

  if [ "$exit_code" -ne 0 ]; then
    echo
    echo "E2E test failed. Container status:"
    "${COMPOSE[@]}" ps || true

    echo
    echo "Container logs:"
    "${COMPOSE[@]}" logs --no-color || true
  fi

  echo
  echo "Stopping E2E environment..."
  "${COMPOSE[@]}" down -v --remove-orphans || true

  exit "$exit_code"
}

trap cleanup EXIT

echo "Validating Compose configuration..."
"${COMPOSE[@]}" config > /dev/null

echo "Starting E2E environment..."
"${COMPOSE[@]}" up -d --wait --wait-timeout 120

echo "Running E2E tests..."
"$ROOT_DIR/e2e/gradlew" \
  -p "$ROOT_DIR/e2e" \
  clean test