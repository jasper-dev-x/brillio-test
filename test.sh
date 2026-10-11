#!/usr/bin/env bash
#
# Runs the test suites.
#
#   ./test.sh              # everything: backend, frontend unit, component, e2e
#   ./test.sh backend      # JUnit 5 / Mockito / MockMvc (./mvnw verify)
#   ./test.sh unit         # frontend lint + typecheck + Jest
#   ./test.sh component    # Cypress component tests (headless)
#   ./test.sh e2e          # Playwright full-stack tests (starts API + UI automatically)
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="${1:-all}"

step() { printf '\n\033[34m▶ %s\033[0m\n' "$*"; }

backend() {
  step "Backend: ./mvnw verify"
  (cd "$ROOT/backend" && ./mvnw -q verify)
}

frontend_deps() {
  cd "$ROOT/frontend"
  if [ ! -d node_modules ] || [ package-lock.json -nt node_modules/.package-lock.json ]; then
    step "Installing frontend dependencies"
    npm install
  fi
}

unit() {
  frontend_deps
  step "Frontend: lint"
  npm run lint
  step "Frontend: typecheck (app, Jest, Cypress, Playwright)"
  npm run typecheck
  step "Frontend: Jest"
  npm test
}

component() {
  frontend_deps
  # The Cypress binary is downloaded by npm postinstall; install it if that was skipped.
  npx cypress verify >/dev/null 2>&1 || npx cypress install
  step "Frontend: Cypress component tests"
  npm run test:component
}

e2e() {
  frontend_deps
  npx playwright install chromium
  step "End-to-end: Playwright (Chromium)"
  npm run test:e2e
}

case "$TARGET" in
  backend)   backend ;;
  unit)      unit ;;
  component) component ;;
  e2e)       e2e ;;
  all)       backend; unit; component; e2e ;;
  *) echo "Unknown target '$TARGET'. Use: backend | unit | component | e2e | all"; exit 2 ;;
esac

printf '\n\033[32m✓ %s tests passed\033[0m\n' "$TARGET"
