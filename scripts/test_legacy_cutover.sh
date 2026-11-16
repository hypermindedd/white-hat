#!/usr/bin/env bash
set -euo pipefail
repo="${1:?Usage: legacy-cutover-drill.sh /absolute/white-hat}"
bin="${WH_C2_SERVER_BIN:?Provide the PostgreSQL 16 server bin directory}"
export WH_C2_PG_BIN="${WH_C2_PG_BIN:-$bin}"
first="${WH_C2_FIRST_PORT:-35445}"
second="${WH_C2_SECOND_PORT:-35446}"
tmp="$(mktemp -d)"
cleanup(){
 for name in source target; do "$bin/pg_ctl" -D "$tmp/$name" stop -m fast >/dev/null 2>&1 || true; done
 rm -rf "$tmp"
}
trap cleanup EXIT
for name in source target; do "$bin/initdb" -D "$tmp/$name" -U postgres --auth=trust --no-locale >/dev/null; done
"$bin/pg_ctl" -D "$tmp/source" -o "-h 127.0.0.1 -p $first -c unix_socket_directories=" -l "$tmp/source.log" -w start
"$bin/pg_ctl" -D "$tmp/target" -o "-h 127.0.0.1 -p $second -c unix_socket_directories=" -l "$tmp/target.log" -w start
export WH_C2_ORIGIN_DB=postgresql://postgres@127.0.0.1:"$first"/postgres
export WH_C2_RESTORED_DB=postgresql://postgres@127.0.0.1:"$second"/postgres
while IFS= read -r f; do
 "$WH_C2_PG_BIN/psql" -Xq -v ON_ERROR_STOP=1 "$WH_C2_ORIGIN_DB" -f "$f"
done < <(find "$repo/server/adapters/src/main/resources/db/migration" -maxdepth 1 -name 'V*.sql' | sort -V)
node --test "$repo/apps/web-bff/test/legacy-cutover.test.mjs"
