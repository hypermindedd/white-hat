# White Hat iOS host

The SwiftUI target uses the shared Kotlin client through `IosHostClient`.
The host supplies PKCE proofs (CryptoKit/Security), HTTPS browser callbacks
(ASWebAuthenticationSession), RS256 ID token verification against the configured
JWKS, and device-local Keychain storage. The UI supports sign-in, workspace
selection and creation, decisions, and starting, refreshing and cancelling
analysis. The target requires iOS 17.4 or later.

## Build on macOS

Use Xcode, XcodeGen and JDK 21. Run from the repository root.

```sh
./gradlew :shared:client:assembleWhiteHatClientDebugXCFramework
xcodegen generate --spec apps/ios/project.yml
xcodebuild -project apps/ios/WhiteHat.xcodeproj -scheme WhiteHat \
  -configuration Debug -destination 'generic/platform=iOS Simulator' \
  ARCHS=arm64 CODE_SIGNING_ALLOWED=NO build
```

The candidate workflow performs this unsigned simulator build. The Linux
Kotlin compiler can compile the two iOS libraries. Xcode handles the framework
link and Swift type checking and application build.

## Configure acceptance

Provide the public build settings `WHITE_HAT_API`, `WHITE_HAT_ISSUER`,
`WHITE_HAT_CLIENT_ID`, `WHITE_HAT_AUTHORIZATION`, `WHITE_HAT_TOKEN`,
`WHITE_HAT_REVOCATION`, `WHITE_HAT_JWKS`, and `WHITE_HAT_REDIRECT_HOST`.
Use the HTTPS API base with its /api/v1 suffix. Register the exact
`https://<host>/oauth/ios` redirect on the iOS public client. Configure the
callback host's associated-domain file for the final team and bundle ID
(`webcredentials`). Supply the intended bundle ID and development team in
the signed-device build. These settings must use the approved test tenant.

On the signed app, record browser callback success, cancellation and rejection
of invalid state, Keychain persistence across restarts, logout clearing the
session, workspace and analysis operations, clean install and upgrade. Capture
the app version, exact commit, device/OS and signing identity. Keep access
tokens, authorization codes and private signing material out of the evidence.
