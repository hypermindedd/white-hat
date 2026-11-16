# White Hat | S2 — Web BFF + Kotlin-facing panel

**Status: tested fixture slice, NOT production-ready.** Node 24 serves a web-only BFF and DOM interface. Core decisions, permissions, membership, idempotency, runs, usage, and PostgreSQL remain wholly in existing Kotlin API v1; there is **no new decision engine, second DB, or alpha SQLite writer**. The immutable recovered alpha source remains under `apps/second-mind/` and is not overwritten. The CSS and icon are read from that recovered source; the new web DOM adapter is under `apps/web-bff/public/`.

## Start and configuration

From repository root:

```bash
cd apps/web-bff
npm test
npm run check
# Set required WH_WEB_ORIGIN, WH_OIDC_ISSUER, WH_OIDC_CLIENT_ID, WH_API_BASE via environment/secret manager.
npm start
```

Use Node 24.15+ and a separately running Kotlin API on `WH_API_BASE`. The `.env.example` is a template and is **not** auto-loaded. For *loopback-only local* tests, `WH_DEV_INSECURE_HTTP=1` permits an HTTP web origin / issuer only at `127.0.0.1`. Outside that exceptional fixture setting, both web origin and IdP URLs must be HTTPS. Production must terminate TLS at a trusted reverse proxy; the app must not be placed behind an untrusted public HTTP front end. `WH_WEB_ORIGIN` must be the exact public origin (not path). Configure the IdP to allow `<origin>/auth/callback` and issue access tokens accepted by the existing Kotlin API (issuer, RS256, audience, `token_use=access`), not only a valid OIDC ID token.

## Security contract

- Authorization Code + S256 PKCE, cryptographic state and nonce, short-lived pending transactions; callback rejects replay and bad state.
- OIDC discovery against a pinned issuer, RS256 JWKS verification of the ID token including `iss`, `aud`, `sub`, `exp`, `iat`, and login `nonce`; rejected token never creates a session.
- Server-only access/refresh tokens. Browser receives HttpOnly / SameSite=Lax / Secure in HTTPS `__Host-` session cookie and short-lived CSRF value from `GET /bff/session`. No localStorage bearer token; no AI secret in JS.
- Origin + CSRF checked for mutating BFF endpoints; upstream Bearer attached server-side only, allowed URL patterns and methods are explicit. No general reverse proxy, no CORS, no secrets in error responses; upstream error status and request_id preserved.
- Token refresh is serialized per session using the configured store (PostgreSQL row locking across replicas; an in-memory chain only for local fixtures); rotation retained on refresh. POST logout invalidates local session and attempts registered IdP refresh-token revocation where supported. Revocation result is reported, not falsely guaranteed.
- Session has an absolute 8h lifetime. `request_id` is per redacted BFF error; Kotlin request IDs pass through on proxied JSON responses. Strict CSP and no-store responses.

**Session storage:** outside explicit loopback development, `WH_WEB_SESSION_DATABASE_URL` and `WH_WEB_SESSION_KEYS` are required. Apply `db/session-store.sql` with the schema owner in the dedicated session database; grant the BFF role data access to its three tables only. The first comma-separated canonical Base64 32-byte key encrypts AES-256-GCM payloads; subsequent keys allow reads during coordinated rotation. All replicas need compatible key sets. Login state is consumed once, and row locks serialize session refresh across replicas. This database owns authentication transport only, never workspace permissions or decisions.

**Production gates:** staging TLS/proxy, approved real IdP and JWKS rotation, full BFF refresh/failover acceptance, ingress abuse controls, consent/retention, security and load testing remain open. Independent stores and ciphertext tests do not prove complete deployment failover or real-provider behavior.

## Panel ↔ API integration

| Current feature | Kotlin API route | Caveat |
|---|---|---|
| Login, refresh, logout | OIDC + `/bff/session` | live IdP untested |
| Workspace listing | `GET /api/v1/me` | subject from JWT |
| Workspace creation | `POST /api/v1/workspaces` | backend OWNER authority |
| Seven Mode selection | `GET /api/v1/modes` | stable wire IDs |
| Decision list/create/read/update | workspace-scoped decisions routes | `expected_revision` for PUT |
| Start and poll run | decisions `POST /runs`; workspace `GET /runs/{run}` | idempotency key, explicit status and errors |
| Cancel run | `POST /runs/{run}/cancel` | cancellation does not imply free provider usage |
| Membership roles | `POST /members` | OWNER controls membership; server-authorized member-list route exists |
| Aggregate usage | `GET /admin/usage` | OWNER/ADMIN only; null stays unknown |

Server-authorized `GET /workspaces/{workspace}/runs` and `/members` routes provide run history and active membership. The DOM adapter reads these routes; Kotlin controls private-data and role boundaries. Their presence does not establish acceptance of the authoritative comprehensive panel, whose source pin remains open. `reconciliation_required` is visibly distinguished from success; no automatic paid retries. API 401/403/404/409/429 are preserved and rendered as errors, although error-code-specific copy requires additional full acceptance testing.

Support roles, operator permissions, quality/feedback queues, usage/cost views, RLS, synthetic legacy-cutover tooling and native hosts exist in the Stage C candidate. Their full acceptance, privacy-sensitive analytics, live voice, real billing and real AI quality remain separate gates. No recovered alpha password is federated automatically with OIDC. Do not enable privacy-sensitive analytics/AI training without explicit consent and retention controls.

## Test boundary

`npm test` runs synthetic OIDC/BFF and session-store regressions. Set `WH_SESSION_TEST_DB` to a disposable PostgreSQL test database to execute the independent-instance security tests; they create and remove a unique schema. The Web workflow supplies PostgreSQL 16 explicitly. Without this variable the database suite is SKIPPED, never a pass. These tests cover state consumption, expiry, concurrent updates, persistence across store recreation, revocation, key rollover, ciphertext tampering/transplantation, transaction rollback and a shared rate limit.

The separate Kotlin workflow runs `test/e2e/stage-c-browser.mjs` against real Ktor and PostgreSQL using signed synthetic identity and provider receipts. It also runs independent legacy cutover/restore and shared-client lifecycle fixtures. No real model, real IdP or device acceptance follows from those results. Current source and raw receipts are recorded in `evidence/stage-c-review/` and `docs/stages/S2-BLOCKERS-FA.md`; earlier S1/S2 receipts are historical.
