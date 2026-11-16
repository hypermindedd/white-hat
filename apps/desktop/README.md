# White Hat Compose Desktop

This development host connects the C5 shared client to the browser-based
OIDC flow and exposes workspace, decision and analysis operations.

Set the following environment variables for the approved test deployment.

| Variable | Value |
|---|---|
| WH_DESKTOP_API | HTTPS API base ending in /api/v1 |
| WH_DESKTOP_ISSUER | Exact HTTPS OIDC issuer |
| WH_DESKTOP_CLIENT_ID | Registered public client ID |
| WH_DESKTOP_AUTHORIZATION_URL | HTTPS authorization endpoint |
| WH_DESKTOP_TOKEN_URL | HTTPS token endpoint |
| WH_DESKTOP_REVOCATION_URL | HTTPS revocation endpoint |
| WH_DESKTOP_JWKS_URL | Approved HTTPS JWKS endpoint |
| WH_DESKTOP_REDIRECT_URI | Registered IPv4 loopback callback including its port and path |

On macOS the app keeps the session in the user's Keychain under
`whitehat.desktop.session.v1`. On Windows the app uses user-scoped DPAPI and
stores only the protected blob under `%LOCALAPPDATA%/WhiteHat/credentials`.
Each issuer, public client ID and API combination has a separate entry.
Linux development builds hold the session in memory. On relaunch the app
attempts to restore the profile from a saved session. Sign out removes
that session locally before requesting revocation on the IdP.

Run `./gradlew :apps:desktop:test` to check the credential adapter. The
native round-trip runs automatically on Windows. On macOS, run
`WH_TEST_MAC_KEYCHAIN=1 ./gradlew :apps:desktop:test --rerun-tasks`
from an unlocked interactive login session. Also verify a real sign-in,
relaunch, upgrade and sign-out with the installed signed app.

Use `./gradlew :apps:desktop:run` to launch. Build the package on its target
host with `packageDeb`, `packageDmg`, or `packageMsi`. The Gradle property
`whiteHatPackageVersion` controls the installer version. The GitHub workflow
builds development installers and needs to run on the published candidate.
