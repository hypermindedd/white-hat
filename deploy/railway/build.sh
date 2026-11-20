#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.."

./gradlew --no-daemon :server:api:installDist
runtime=build/railway
mkdir -p "$runtime/api" "$runtime/web" "$runtime/bin"
install -m 0755 "$(node -p process.execPath)" "$runtime/bin/node"
cp -a server/api/build/install/api/. "$runtime/api/"
cp -a apps/web-bff/{package.json,package-lock.json,server.mjs,session-store.mjs,public,db} "$runtime/web/"
cp deploy/railway/start.sh "$runtime/start.sh"
cp deploy/railway/bootstrap-db.mjs "$runtime/web/"
npm ci --omit=dev --ignore-scripts --prefix "$runtime/web"
