# Browser integration evidence scope

The supplied temporary harness is now `apps/web-bff/test/e2e/stage-c-browser.mjs`, invoked by `stageCBrowserWithRealKtorAndPostgres` with `WH_BROWSER_E2E=true` in Kotlin CI. It uses actual Chromium, Node BFF, Ktor and embedded PostgreSQL, a locally signed synthetic identity issuer, and synthetic provider receipts.

Assertions cover five roles/outsider, private decision isolation, same-session support grant/revoke/deprovision, encrypted stored sessions, logout/revocation, mobile RTL/no horizontal overflow, keyboard focus, controls and receipt visibility. Output is a CI artifact under `server/api/build/browser-evidence/`.

A green harness does not cover every seven-mode browser route, screen-reader/axe audit, production TLS/live IdP, refresh rotation/replay across replicas, reconnect, or real AI. Those remain distinct acceptance rows. Read the latest exact-HEAD workflow result before assigning PASS. Until then status is NOT RUN.

