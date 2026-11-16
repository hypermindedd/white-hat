# White Hat Android development host

Build with JDK 21 and Android SDK 35.

```sh
./gradlew -PwhiteHatAndroid=true :apps:android:assembleDebug :apps:android:assembleDebugAndroidTest
```

Set the following Gradle properties in your local or CI configuration.
These values are public configuration embedded in the APK.

| Property | Value |
|---|---|
| whiteHatAndroidOidcHost | HTTPS App Link host with an association file |
| whiteHatAndroid_API | HTTPS API base ending in /api/v1 |
| whiteHatAndroid_ISSUER | Exact HTTPS OIDC issuer |
| whiteHatAndroid_CLIENT_ID | Android public client ID |
| whiteHatAndroid_AUTHORIZATION_URL | HTTPS authorization endpoint |
| whiteHatAndroid_TOKEN_URL | HTTPS token endpoint |
| whiteHatAndroid_REVOCATION_URL | HTTPS revocation endpoint |
| whiteHatAndroid_JWKS_URL | Approved HTTPS JWKS endpoint |

Register `https://<host>/oauth/android` exactly with the IdP and publish
`/.well-known/assetlinks.json` on the host for the package and signing
certificate actually used. Use separate development and distribution
signing registrations as appropriate. App Link verification can be checked on
the target device with `adb shell pm get-app-links whitehat.android`.

The debug app displays a configuration message until settings are supplied.
It uses the system browser and a single in-process authorization attempt.
If Android terminates the app during authorization, retry from the app.
The store uses a non-exportable Android Keystore AES key and GCM-protected
app-private preferences. Use a dedicated synthetic environment to exercise
the instrumented storage test.

```sh
./gradlew -PwhiteHatAndroid=true :apps:android:connectedDebugAndroidTest
```

Record version, signing fingerprint, verified App Link, login/refresh/logout,
clean install and upgrade, and the actual host/device used. The debug build
certificate is for development acceptance. Release signing and distribution
require the project's approved credentials and account.
