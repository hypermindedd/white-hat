#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.."

version=26.8.2
digest=dd124787283918bc597628887168333465ee2a811a389e0003569e52be7e487f
archive=$(mktemp)
trap 'rm -f -- "$archive"' EXIT
curl --fail --silent --show-error --location --retry 3 \
  "https://github.com/keycloak/keycloak/releases/download/$version/keycloak-$version.tar.gz" --output "$archive"
printf '%s  %s\n' "$digest" "$archive" | sha256sum --check --status
runtime=build/whitehat-idp
mkdir -p "$runtime"
tar -xzf "$archive" --strip-components=1 -C "$runtime"
mkdir -p "$runtime/data/import"
cp deploy/railway/whitehat-pilot-realm.json "$runtime/data/import/"
KC_DB=postgres bash "$runtime/bin/kc.sh" build
