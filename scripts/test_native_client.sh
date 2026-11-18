#!/usr/bin/env bash
set -euo pipefail
repo="${1:?Usage: test_native_client.sh /absolute/white-hat}"
cd "$repo"
./gradlew --no-daemon :server:api:writeNativeClientFixtureClasspath
temporary="$(mktemp -d)"
fixture_pid=""
cleanup(){
 if [[ -n "$fixture_pid" ]]; then kill "$fixture_pid" 2>/dev/null || true; wait "$fixture_pid" 2>/dev/null || true; fi
 rm -rf "$temporary"
}
trap cleanup EXIT
java -cp "$(cat server/api/build/native-client-fixture-classpath.txt)" whitehat.api.NativeClientFixtureKt "$temporary/ready.json" > "$temporary/fixture.log" 2>&1 &
fixture_pid=$!
for ((attempt=0;attempt<150;attempt++)); do
 if [[ -s "$temporary/ready.json" ]]; then break; fi
 if ! kill -0 "$fixture_pid" 2>/dev/null; then cat "$temporary/fixture.log"; exit 1; fi
 sleep 0.2
done
test -s "$temporary/ready.json"
WH_C5_TEST_API="$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["origin"])' "$temporary/ready.json")"
WH_C5_TEST_TOKEN="$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["token"])' "$temporary/ready.json")"
export WH_C5_TEST_API WH_C5_TEST_TOKEN
./gradlew --no-daemon :shared:client:jvmTest --rerun-tasks
