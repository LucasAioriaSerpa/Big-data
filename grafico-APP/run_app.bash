#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="${VENV_DIR:-"$SCRIPT_DIR/.venv"}"
BACKEND_PORT="${BACKEND_PORT:-8000}"
FRONTEND_PORT="${FRONTEND_PORT:-5000}"

if [[ ! -x "$VENV_DIR/bin/python" ]]; then
    python3 -m venv "$VENV_DIR"
fi

"$VENV_DIR/bin/python" -m pip install -q -r "$SCRIPT_DIR/requirements.txt"

export PYTHONPATH="$SCRIPT_DIR/backend/api:$SCRIPT_DIR/frontend${PYTHONPATH:+:$PYTHONPATH}"
export RESULT_FILE="${RESULT_FILE:-"$SCRIPT_DIR/../temperatura-media/src/main/resources/output/output-temperatura-correlacoes/part-r-00000"}"
export FASTAPI_URL="${FASTAPI_URL:-"http://127.0.0.1:$BACKEND_PORT"}"

cleanup() {
    [[ -n "${BACKEND_PID:-}" ]] && kill "$BACKEND_PID" 2>/dev/null || true
    [[ -n "${FRONTEND_PID:-}" ]] && kill "$FRONTEND_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

cd "$SCRIPT_DIR"
"$VENV_DIR/bin/uvicorn" app.main:app --app-dir backend/api --host 127.0.0.1 --port "$BACKEND_PORT" &
BACKEND_PID=$!
"$VENV_DIR/bin/flask" --app frontend.app.run:app run --host 127.0.0.1 --port "$FRONTEND_PORT" &
FRONTEND_PID=$!

printf 'Frontend: http://127.0.0.1:%s\n' "$FRONTEND_PORT"
printf 'Backend:  http://127.0.0.1:%s/docs\n' "$BACKEND_PORT"
wait -n "$BACKEND_PID" "$FRONTEND_PID"
