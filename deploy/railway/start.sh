#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

export WH_HOST=127.0.0.1 WH_PORT=8081
export WH_API_BASE=http://127.0.0.1:8081
export WH_WEB_HOST=0.0.0.0 WH_WEB_PORT="${PORT:-8080}"
export WH_DEV_INSECURE_HTTP=0

api_pid=
web_pid=
stop_children() {
  trap - EXIT
  local pid
  for pid in "$web_pid" "$api_pid"; do
    if [[ -n "$pid" ]]; then kill -TERM "$pid" 2>/dev/null || true; fi
  done
  wait || true
}
trap stop_children EXIT
trap 'exit 143' TERM
trap 'exit 130' INT

./api/bin/api &
api_pid=$!
ready=
for ((second = 0; second < 45; second++)); do
  if ! kill -0 "$api_pid" 2>/dev/null; then
    echo 'Kotlin API exited before readiness' >&2
    exit 1
  fi
  if curl --fail --silent --max-time 1 "$WH_API_BASE/api/v1/ready" >/dev/null; then
    ready=1
    break
  fi
  sleep 1
done
if [[ -z "$ready" ]]; then
  echo 'Kotlin API readiness timed out' >&2
  exit 1
fi

./bin/node ./web/server.mjs &
web_pid=$!
wait -n "$api_pid" "$web_pid" || true
echo 'A White Hat process exited; restarting the service' >&2
exit 1
