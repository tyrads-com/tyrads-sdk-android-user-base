# Push Notifications

This SDK is a pure relay: your app owns requesting notification permission and obtaining an FCM
token (via your own Firebase project setup); the SDK just forwards the token to the backend as
part of login. `devicePushToken` is an **FCM registration token**.

The snippet below uses `.await()` on the Firebase task, which comes from
`org.jetbrains.kotlinx:kotlinx-coroutines-play-services` — add it to your app if you don't have it:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
```

```kotlin
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

val token: String = FirebaseMessaging.getInstance().token.await() // FCM registration token

TyradsUserBase.loginUser(userId, TyradsInitOptions(devicePushToken = token))
```

Call `loginUser` again (with the same `userId`) whenever the token rotates, to keep the backend's
copy current.

On Android 13+, request the `POST_NOTIFICATIONS` runtime permission yourself if you want
notifications to actually display — a token can still be obtained without it:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        // request via an ActivityResultContracts.RequestPermission() launcher
    }
}
```

## What this SDK does **not** do

* Request notification permission.
* Generate or retrieve a push token itself.
* Set up a notification channel.
* Route notification clicks to a deep link automatically.
* Show any notification UI.
