# Push Notifications

Fully automatic. The SDK fetches its own FCM token internally, scoped to TyrAds' own Firebase
project (not whatever Firebase project your app might use), and sends it during `loginUser()`.
Your app doesn't need a Firebase setup for this, doesn't call anything, and never sees the token.

On Android 13+ the SDK asks for the `POST_NOTIFICATIONS` runtime permission once, the first time
`loginUser()` runs, so notifications can actually display. A token is fetched either way.

`TyradsInitOptions.devicePushToken` is deprecated and ignored.

## What this SDK does **not** do

* Set up a notification channel.
* Route notification clicks to a deep link automatically.
* Show any notification UI.
