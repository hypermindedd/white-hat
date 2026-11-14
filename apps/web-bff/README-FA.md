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
- Token refresh is serialized per in-memory session; rotation retained on refresh. POST logout invalidates local session and attempts registered IdP refresh-token revocation where supported. Revocation result is reported, not falsely guaranteed.
- Session has an absolute 8h lifetime. `request_id` is per redacted BFF error; Kotlin request IDs pass through on proxied JSON responses. Strict CSP and no-store responses.

**Production gates:** Memory-only session storage prevents durable multi-replica operation and loses sessions on restart. Replace it with an encrypted, centrally managed session store plus eviction/revocation and abuse controls before live release; verify TLS/proxy headers, real IdP discovery/JWKS rotation and consent/data-handling, perform security and load testing. The current BFF is a verified *single-process alpha slice*, not a production authentication service.

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
| Membership roles | `POST /members` | only OWNER can update; no member-list endpoint yet |
| Aggregate usage | `GET /admin/usage` | OWNER/ADMIN only; null stays unknown |

A run ID returned during the current browser session can be polled and cancelled. **No historical run-list API currently exists**: refresh discards the selected run ID, not its persisted PostgreSQL record. A server-authorized run-list endpoint must be designed and tested before offering full run history. `reconciliation_required` is visibly distinguished from success; no automatic paid retries. API 401/403/404/409/429 are preserved and rendered as errors, although error-code-specific copy requires additional full acceptance testing.

The custom permission builder, support analytics, account-age/geographic analytics, live voice, billing, real AI benchmark, SQLite migration, RLS and native app UI remain separate, unpassed gates. No recovered alpha password is federated automatically with OIDC. Do not enable privacy-sensitive analytics/AI training without explicit consent and retention controls.

## Test boundary

`npm test` runs local network fixtures for OIDC login, PKCE, strict JWT signature, state/replay, CSRF, API route allowlist, preserved 403 and null usage, refresh, revocation, logout. These do **not** attest real IdP, real Kotlin API, real PostgreSQL or browser E2E. On the present execution container, checks ran with Node 22.16 rather than the required Node 24 toolchain; Node 24 validation must run on GitHub Actions or a matching local runtime. Existing 28 Kotlin and 23 alpha Node test results belong to S1, not repeated here.

Source pin before S2: PR #2 head `7429f348c6b84b28c209cd64ed9d9128141af651`; no newer White Hat panel was verified in inspected branches and Notion content. See `docs/stages/S2-IMPLEMENTATION-FA.md` and `evidence/web-bff/report.json`.
