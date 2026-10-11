#!/usr/bin/env bash
# Runs the backend (JUnit) and frontend (Vitest) test suites. Exits non-zero if either fails.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "▶ Backend tests (mvn test)"
(cd "$ROOT/backend" && mvn -q test)

echo "▶ Frontend typecheck + tests"
cd "$ROOT/frontend"
[ -d node_modules ] || npm install
npm run typecheck
npm test

echo "✓ All tests passed"
