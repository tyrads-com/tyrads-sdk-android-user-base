# Tyrads User-Base Example App

Demonstrates every `TyradsUserBase` method against a real backend: login, campaign browsing,
activation, tracking, and the offerwall URL builder. One button per call, with the raw JSON
response shown and a copy-to-clipboard action.

It consumes the SDK exactly like a publisher would, from JitPack
(`com.github.tyrads-com:tyrads-sdk-android-user-base:v1.0.0`), not from the local module.

## Prerequisites

* Android Studio (or the command line + an emulator/device)
* An API key + API secret from the Tyrads Dashboard

No Firebase setup is needed for push: the SDK fetches its own FCM token internally. The
`google-services.json` here only gives the example its own default Firebase app, like a typical
publisher app.

## Branches

| Branch | applicationId | Environment |
|---|---|---|
| `main` | `com.example.androiduserbase.prod` | Production (SDK default) |
| `stag` (internal) | `com.example.androiduserbase.stag` | Staging |

## Run it

**Android Studio:** open the project, select the `example` run configuration, hit Run.

**Command line:**

```sh
./gradlew :example:installDebug
adb shell am start -n com.example.androiduserbase.prod/com.tyrads.sdk.userbase.example.MainActivity
```

## Using the app

1. Fill in **API Key** / **API Secret** (Enc Key is optional, it enables AES-256-GCM secure mode).
2. Tap **Init & Login**. On Android 13+ the SDK asks for notification permission once.
3. Once the session is ready, tap through the rest of the cards. **Campaign Detail** and
   **Activate Campaign** need a Campaign ID: paste one from the **Campaign Recommendation**
   response above.
4. Every response or error is shown as pretty-printed JSON, with a **Copy** button.
