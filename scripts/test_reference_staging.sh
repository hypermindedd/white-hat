#!/usr/bin/env bash
set -euo pipefail
repo="${1:?Supply the repository root}"
[[ "${GITHUB_ACTIONS:-}" == "true" ]] || { echo 'Run only on an isolated GitHub Actions runner; installs a disposable trust anchor.'; exit 2; }
cd "$repo"
temporary="$(mktemp -d)"
fixture_pid=""
container="wh-s2-keycloak-${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
cleanup(){
 if [[ -n "$fixture_pid" ]]; then kill "$fixture_pid" 2>/dev/null || true; wait "$fixture_pid" 2>/dev/null || true; fi
 docker rm -fv "$container" >/dev/null 2>&1 || true
 certutil -D -d "sql:$HOME/.pki/nssdb" -n 'WhiteHat reference CA' >/dev/null 2>&1 || true
 rm -rf "$temporary"
 if docker inspect "$container" >/dev/null 2>&1; then echo 'REFERENCE_CLEANUP_FAILED'; return 1; fi
}
trap cleanup EXIT
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -keyout "$temporary/ca.key" -out "$temporary/ca.crt" -subj '/CN=WhiteHat disposable reference CA' >/dev/null 2>&1
openssl req -newkey rsa:2048 -nodes -keyout "$temporary/server.key" -out "$temporary/server.csr" -subj '/CN=localhost' >/dev/null 2>&1
cat > "$temporary/extensions" <<'EOF'
subjectAltName=DNS:localhost,IP:127.0.0.1
basicConstraints=CA:FALSE
extendedKeyUsage=serverAuth
EOF
openssl x509 -req -in "$temporary/server.csr" -CA "$temporary/ca.crt" -CAkey "$temporary/ca.key" -CAcreateserial -out "$temporary/server.crt" -days 1 -extfile "$temporary/extensions" >/dev/null 2>&1
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -keyout "$temporary/untrusted.key" -out "$temporary/untrusted.crt" -subj '/CN=Untrusted reference CA' >/dev/null 2>&1
# Keycloak runs as a different uid inside this disposable runner. None of these keys is uploaded.
chmod a+r "$temporary/server.key" "$temporary/server.crt"
chmod 755 "$temporary"
mkdir -p "$HOME/.pki/nssdb"
if ! certutil -L -d "sql:$HOME/.pki/nssdb" >/dev/null 2>&1; then certutil -N --empty-password -d "sql:$HOME/.pki/nssdb"; fi
certutil -A -d "sql:$HOME/.pki/nssdb" -n 'WhiteHat reference CA' -t 'C,,' -i "$temporary/ca.crt"
keytool -importcert -noprompt -alias whitehat-reference -file "$temporary/ca.crt" -keystore "$temporary/truststore.p12" -storepass changeit >/dev/null 2>&1
export NODE_EXTRA_CA_CERTS="$temporary/ca.crt"
node scripts/reference-staging.mjs --prepare "$temporary"
docker pull quay.io/keycloak/keycloak:26.4.7 >/dev/null
mkdir -p server/api/build/reference-staging
docker image inspect quay.io/keycloak/keycloak:26.4.7 --format '{{json .RepoDigests}}' > server/api/build/reference-staging/keycloak-image.json
docker run -d --name "$container" -p 127.0.0.1:38443:8443 \
 --env-file "$temporary/keycloak.env" \
 -v "$temporary/server.crt:/opt/keycloak/conf/server.crt:ro" \
 -v "$temporary/server.key:/opt/keycloak/conf/server.key:ro" \
 -v "$temporary/realm:/opt/keycloak/data/import:ro" \
 quay.io/keycloak/keycloak:26.4.7 start-dev --import-realm --http-enabled=false \
 --hostname=https://localhost:38443 --https-certificate-file=/opt/keycloak/conf/server.crt \
 --https-certificate-key-file=/opt/keycloak/conf/server.key >/dev/null
ready_idp=0
for ((attempt=0;attempt<120;attempt++)); do
 if curl -fsS --cacert "$temporary/ca.crt" https://localhost:38443/realms/whitehat-s2/.well-known/openid-configuration >/dev/null 2>&1; then ready_idp=1; break; fi
 sleep 1
done
if [[ "$ready_idp" != "1" ]]; then docker logs "$container"; exit 1; fi
./gradlew --no-daemon :server:api:writeNativeClientFixtureClasspath
export WH_S2_ISSUER=https://localhost:38443/realms/whitehat-s2
export WH_S2_SUBJECT=11111111-2222-4333-8444-555555555555
java -Djavax.net.ssl.trustStore="$temporary/truststore.p12" -Djavax.net.ssl.trustStorePassword=changeit \
 -cp "$(cat server/api/build/native-client-fixture-classpath.txt)" whitehat.api.ReferenceStagingFixtureKt "$temporary/ready.json" > "$temporary/fixture.log" 2>&1 &
fixture_pid=$!
for ((attempt=0;attempt<150;attempt++)); do
 if [[ -s "$temporary/ready.json" ]]; then break; fi
 if ! kill -0 "$fixture_pid" 2>/dev/null; then cat "$temporary/fixture.log"; exit 1; fi
 sleep 0.2
done
if [[ ! -s "$temporary/ready.json" ]]; then cat "$temporary/fixture.log"; exit 1; fi
node scripts/reference-staging.mjs --test "$temporary"
cleanup
fixture_pid=""
echo 'S2_REFERENCE_CLEANUP_COMPLETE — disposable database, realm and local credentials removed'
