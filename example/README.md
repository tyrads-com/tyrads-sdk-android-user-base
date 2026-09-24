# Tyrads User-Base — Example App

Demonstrates every `TyradsUserBase` method against a real backend: login, campaign browsing,
activation, tracking, and the offerwall URL builder — one button per call, with the raw JSON
response shown and a copy-to-clipboard action.

## Prerequisites

* Android Studio (or the command line + an emulator/device)
* An API key + API secret from the Tyrads Dashboard
* (Optional) a Firebase project with an Android app registered for
  `com.tyrads.sdk.userbase.example`, if you want the Device Push Token field to auto-fetch a real
  FCM token — see [Push Notifications](../docs/initialization/push-notifications.md). Without it,
  paste a token manually or leave the field blank.

## Run it

**Android Studio:** open the project, select the `example` run configuration, hit Run.

**Command line:**

```sh
./gradlew :example:installDebug
adb shell am start -n com.tyrads.sdk.userbase.example/.MainActivity
```

## Using the app

1. Fill in **API Key** / **API Secret** (Enc Key is optional — enables AES-256-GCM secure mode).
2. Tap **Init & Login**.
3. Once the session is ready, tap through the rest of the cards — **Campaign Detail** and
   **Activate Campaign** need a Campaign ID; paste one from the **Campaign Recommendation**
   response above.
4. Every response or error is shown as pretty-printed JSON, with a **Copy** button.

Environment (Production/Staging) locks after the first **Init & Login** — restart the app to
switch.
