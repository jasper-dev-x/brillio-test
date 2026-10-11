#!/usr/bin/env bash
#
# Runs the Spring Boot API and the Vite dev server together for local development.
#
#   ./dev.sh                      # API on :8080, UI on :5173
#   BACKEND_PORT=9090 ./dev.sh    # custom API port (the UI proxy follows it)
#
# Ctrl+C stops both.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

red()   { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
blue()  { printf '\033[34m%s\033[0m\n' "$*"; }

fail() { red "✗ $*"; exit 1; }

# ---------- prerequisite checks ----------
command -v java >/dev/null 2>&1 || fail "Java not found. Install JDK 17+ (e.g. 'brew install openjdk@21')."
JAVA_MAJOR="$(java -version 2>&1 | awk -F'"' '/version/ {split($2, v, "."); print (v[1] == "1" ? v[2] : v[1])}')"
[ "${JAVA_MAJOR:-0}" -ge 17 ] 2>/dev/null || fail "Java 17+ required (found ${JAVA_MAJOR:-unknown})."

if command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
elif [ -x "$ROOT/backend/mvnw" ]; then
  MVN="$ROOT/backend/mvnw"
else
  fail "Maven not found. Install it with 'brew install maven'."
fi

command -v node >/dev/null 2>&1 || fail "Node.js not found. Install Node 22.12+ (e.g. 'nvm install 22')."
NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
NODE_MINOR="$(node -p 'process.versions.node.split(".")[1]')"
if [ "$NODE_MAJOR" -lt 22 ] || { [ "$NODE_MAJOR" -eq 22 ] && [ "$NODE_MINOR" -lt 12 ]; }; then
  fail "Node 22.12+ required (found $(node -v)). Try 'nvm install 22 && nvm use 22'."
fi

port_in_use() { lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }
port_in_use "$BACKEND_PORT"  && fail "Port $BACKEND_PORT is already in use (set BACKEND_PORT to change it)."
port_in_use "$FRONTEND_PORT" && fail "Port $FRONTEND_PORT is already in use (set FRONTEND_PORT to change it)."

# ---------- install frontend deps on first run / when the lockfile changes ----------
cd "$ROOT/frontend"
if [ ! -d node_modules ] || [ package-lock.json -nt node_modules/.package-lock.json ]; then
  blue "• Installing frontend dependencies..."
  npm install
fi

# ---------- process management ----------
# Kill the whole process group (Maven forks a JVM; Vite spawns esbuild) on exit or Ctrl+C.
cleanup() {
  trap - INT TERM EXIT
  echo
  blue "• Shutting down..."
  kill -- -$$ 2>/dev/null || true
}
trap cleanup INT TERM EXIT

prefix() { awk -v p="$1" '{ print p $0; fflush() }'; }

# ---------- backend ----------
blue "• Starting backend on http://localhost:$BACKEND_PORT ..."
(
  cd "$ROOT/backend"
  "$MVN" -q spring-boot:run -Dspring-boot.run.arguments="--server.port=$BACKEND_PORT" 2>&1 | prefix $'\033[35m[api]\033[0m '
) &

# Wait until the API answers so the UI's first request doesn't fail.
# The first run can take a while because Maven downloads dependencies.
for i in $(seq 1 180); do
  if curl -fsS "http://localhost:$BACKEND_PORT/api/cities" >/dev/null 2>&1; then
    green "✓ Backend is up"
    break
  fi
  [ "$i" -eq 180 ] && fail "Backend did not start within 3 minutes (see [api] logs above)."
  sleep 1
done

# ---------- frontend ----------
blue "• Starting frontend on http://localhost:$FRONTEND_PORT ..."
(
  cd "$ROOT/frontend"
  API_URL="http://localhost:$BACKEND_PORT" npx vite --port "$FRONTEND_PORT" --strictPort 2>&1 | prefix $'\033[36m[web]\033[0m '
) &

green "✓ Open http://localhost:$FRONTEND_PORT  (Ctrl+C to stop both)"
wait
