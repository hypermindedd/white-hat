#!/usr/bin/env bash
set -euo pipefail
repo="${1:?Usage: legacy-cutover-drill.sh /absolute/white-hat}"
rehearsal_started="$(date +%s)"
bin="${WH_C2_SERVER_BIN:?Provide the PostgreSQL 16 server bin directory}"
export WH_C2_PG_BIN="${WH_C2_PG_BIN:-$bin}"
first="${WH_C2_FIRST_PORT:-35445}"
second="${WH_C2_SECOND_PORT:-35446}"
tmp="$(mktemp -d)"
logs="${WH_C2_LOG_DIR:-$tmp}"
mkdir -p "$logs"
cleanup(){
 for name in source target; do "$bin/pg_ctl" -D "$tmp/$name" stop -m fast >/dev/null 2>&1 || true; done
 rm -rf "$tmp"
}
trap cleanup EXIT
for name in source target; do "$bin/initdb" -D "$tmp/$name" -U postgres --auth=trust --no-locale >/dev/null; done
start_cluster() {
 local name="$1" port="$2" log="$logs/$1.log" status
 if "$bin/pg_ctl" -D "$tmp/$name" -o "-h 127.0.0.1 -p $port -c unix_socket_directories=" -l "$log" -w start; then
  return 0
 else
  status=$?
 fi
 printf 'PostgreSQL %s cluster failed to start on port %s (exit %s)\n' "$name" "$port" "$status" >&2
 if [[ -s "$log" ]]; then tail -n 50 "$log" >&2; fi
 return "$status"
}
start_cluster source "$first"
start_cluster target "$second"
export WH_C2_ORIGIN_DB=postgresql://postgres@127.0.0.1:"$first"/postgres
export WH_C2_RESTORED_DB=postgresql://postgres@127.0.0.1:"$second"/postgres
while IFS= read -r f; do
 "$WH_C2_PG_BIN/psql" -Xq -v ON_ERROR_STOP=1 "$WH_C2_ORIGIN_DB" -f "$f"
done < <(find "$repo/server/adapters/src/main/resources/db/migration" -maxdepth 1 -name 'V*.sql' | sort -V)
node --test "$repo/apps/web-bff/test/legacy-cutover.test.mjs"
rehearsal_duration="$(( $(date +%s) - rehearsal_started ))"
test "$rehearsal_duration" -le 120
python3 -c 'import json,sys;print(json.dumps({"check":"reference_recovery_budget","pass":True,"rehearsal_seconds":int(sys.argv[1]),"rto_upper_bound_seconds":120,"rpo_checkpoint_writes":0,"scope":"small synthetic two-cluster fixture, not production sizing"}))' "$rehearsal_duration"
