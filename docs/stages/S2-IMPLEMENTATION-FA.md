# S2 checkpoint — Web ↔ Kotlin integration (not stage closure)

Date: 2026-11-11. Source of truth: `hypermindedd/white-hat` PR #2; pre-change SHA `7429f348c6b84b28c209cd64ed9d9128141af651`.

## Source reconciliation

1. The PR's recovered alpha: `apps/second-mind/public/app.js`, `index.html`, `style.css`, `icon.svg`, `server/app.mjs`; the old application invokes SQLite-backed `/api/*` and must **not** be repointed blindly to Kotlin.
2. The Kotlin server API: `server/{domain,application,adapters,api}`, `contracts/openapi/openapi.json`. Preserve its `/api/v1` contract and `legacy-alpha.1` output label.
3. Inspected GitHub `main`, `build/second-mind-rc0-2026-11-03`, `build/kotlin-platform-foundation-20261111` branches: only the last has the recovered `apps/second-mind` source. Notion `SECOND MIND — V1 Build Project` and design/spec references were discoverable, not a more recent executable panel. More recent comprehensive panel mockups might exist outside accessible source repositories; source pin remains **unverified** beyond this repository.
4. Do not pull in Elyora's administrator panel, identity domain or live-engine persona. Preserve live negotiation design branch as a separate pending reconciliation.

## Implementation

`apps/web-bff` adds a Node transport-only BFF and a browser-facing DOM adapter. The adapter retains the recovered alpha CSS/icon without modifying archived files, while all new actions call existing Kotlin API v1 only. The server BFF is intentionally a thin auth/proxy adapter: no new data store, provider, mode logic or permission authority.

The BFF implements OIDC Code+PKCE, OIDC ID-token verification, server-side token/refresh handling, cookie session, CSRF, origin check, logout with revocation attempt, and strict API allowlist. Browser screens cover workspace, seven modes, decision create/edit, run/poll/cancel, membership update and usage. Endpoint absence is shown explicitly (member list, historical runs, role builder). Null usage values remain unknown and reconciliation requests are not auto-retried.

## Evidence and open gates

See `evidence/web-bff/report.json`. Local tests use a mock OIDC issuer and mock Kotlin HTTP responses. No browser was launched; live IdP, actual Kotlin integration, real PostgreSQL, CI and production security cannot be marked PASS. Pre-existing Actions runs #43022447747 and #43022999862 both failed with no step metadata and no retrievable logs; error cannot yet be attributed to source. Do not claim CI passed because fixture tests passed.

Next gates: (a) obtain and resolve GitHub Actions scheduler/check annotations; (b) integrate real Kotlin/Ktor and actual test PostgreSQL with a fixture issuer; (c) full browser E2E including RTL, keyboard/screen reader, reconnect/cancel, tenancy/role denial; (d) durable distributed web session store, real IdP, policy and security review; (e) operator reconciliation UI, complete permission builder, support access review, migration rehearsal with backup/rollback; (f) human/AI/native benchmarks and independent sign-off.

Rollback before live cutover: revert this S2 commit/PR; since this step does not migrate live data or deploy, no active data rollback is required. Never rollback a future data cutover without ADR-010's backup and replay plan.

**Continuation chat name:** `White Hat | S2B — Integration E2E, CI and Access Control`.

**Paste-ready continuation prompt:** Inspect latest PR #2 head of `hypermindedd/white-hat`, read `KOTLIN-START-HERE-FA.md`, `apps/web-bff/README-FA.md`, `docs/stages/S2-IMPLEMENTATION-FA.md`, `evidence/web-bff/report.json` and GitHub Actions for the latest commit. Preserve existing Kotlin API and alpha assets. Confirm real Node24 tests and CI logs. Run actual API+BFF+local-IdP integration and browser E2E (login, workspace, all modes, revision conflict, 401/403/404/409/429, cancel/reconnect, RTL/keyboard). Before production, implement durable sessions, privileged support role builder, member/run listing APIs under server-side authorization, and genuine reconciliation operator workflow with tests. Update source/evidence in the same PR; never claim PASS without evidence or mix in Elyora. Create next-stage checkpoint when justified.
