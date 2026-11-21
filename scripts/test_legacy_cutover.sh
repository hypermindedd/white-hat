#!/usr/bin/env bash
set -euo pipefail
repo="${1:?Usage: legacy-cutover-drill.sh /absolute/white-hat}"
rehearsal_started="$(date +%s)"
bin="${WH_C2_SERVER_BIN:?Provide the PostgreSQL 16 server bin directory}"
export WH_C2_PG_BIN="${WH_C2_PG_BIN:-$bin}"
tmp="$(mktemp -d)"
logs="${WH_C2_LOG_DIR:-$tmp}"
mkdir -p "$logs"
cleanup(){
 for name in source target; do "$bin/pg_ctl" -D "$tmp/$name" stop -m fast >/dev/null 2>&1 || true; done
 rm -rf "$tmp"
}
trap cleanup EXIT
for name in source target; do
 mkdir -p "$tmp/$name-socket"
 "$bin/initdb" -D "$tmp/$name" -U postgres --auth=trust --no-locale >/dev/null
done
start_cluster() {
 local name="$1" log="$logs/$1.log" status
 if "$bin/pg_ctl" -D "$tmp/$name" -o "-c listen_addresses= -k \"$tmp/$name-socket\"" -l "$log" -w start; then
  return 0
 else
  status=$?
 fi
 printf 'PostgreSQL %s cluster failed to start on its private socket (exit %s)\n' "$name" "$status" >&2
 if [[ -s "$log" ]]; then tail -n 50 "$log" >&2; fi
 return "$status"
}
socket_dsn() {
 python3 -c 'import sys; from urllib.parse import urlencode; print("postgresql://postgres@/postgres?" + urlencode({"host": sys.argv[1]}))' "$1"
}
start_cluster source
start_cluster target
export WH_C2_ORIGIN_DB="$(socket_dsn "$tmp/source-socket")"
export WH_C2_RESTORED_DB="$(socket_dsn "$tmp/target-socket")"
while IFS= read -r f; do
 "$WH_C2_PG_BIN/psql" -Xq -v ON_ERROR_STOP=1 "$WH_C2_ORIGIN_DB" -f "$f"
done < <(find "$repo/server/adapters/src/main/resources/db/migration" -maxdepth 1 -name 'V*.sql' | sort -V)
node --test "$repo/apps/web-bff/test/legacy-cutover.test.mjs"
rehearsal_duration="$(( $(date +%s) - rehearsal_started ))"
test "$rehearsal_duration" -le 120
python3 -c 'import json,sys;print(json.dumps({"check":"reference_recovery_budget","pass":True,"rehearsal_seconds":int(sys.argv[1]),"rto_upper_bound_seconds":120,"rpo_checkpoint_writes":0,"scope":"small synthetic two-cluster fixture, not production sizing"}))' "$rehearsal_duration"
