# Android signing

FG Machines RCK uses a private stable signing identity for the production package:

`com.fgmachines.rck`

The signing key must never be committed to this public repository.

## Required environment variables

For a local signed release build:

- `FG_RCK_KEYSTORE_PATH`
- `FG_RCK_KEYSTORE_PASSWORD`
- `FG_RCK_KEY_ALIAS`
- `FG_RCK_KEY_PASSWORD`

Run:

```bash
gradle --no-daemon clean testDebugUnitTest lintDebug assembleRelease
```

## GitHub Actions secrets

To let GitHub Actions produce the same update-compatible release APK, configure these repository secrets:

- `FG_RCK_KEYSTORE_B64` — base64 of the private keystore file
- `FG_RCK_KEYSTORE_PASSWORD`
- `FG_RCK_KEY_ALIAS`
- `FG_RCK_KEY_PASSWORD`
- `FG_RCK_CERT_SHA256` — SHA-256 fingerprint of the signing certificate

The workflow verifies the resulting APK certificate before uploading the stable release artifact.

## One-time migration

Older FG Machines RCK test APKs were built with ephemeral GitHub runner debug keys. Android therefore cannot update them with the new stable production key.

A device that still has one of those old test builds needs one uninstall/reinstall transition to the first stable-signed APK. After that transition, all later production APKs signed with the same private key can update in place with `adb install -r` or normal package installation.

Debug CI builds intentionally use `com.fgmachines.rck.debug` so they install side-by-side and never collide with the production package.
