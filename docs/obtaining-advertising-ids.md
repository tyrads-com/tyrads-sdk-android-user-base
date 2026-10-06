# Obtaining Advertising ID's

**Manifest permission**

This SDK declares the `AD_ID` permission itself (required by Google Play for apps targeting API 33+
that access the advertising ID) — no extra step needed in your app's manifest:

```xml
<uses-permission android:name="com.google.android.gms.permission.AD_ID" />
```

**Retrieving GAID**

This SDK calls Google Play Services' `AdvertisingIdClient.getAdvertisingIdInfo()` on your behalf
during `loginUser()` — you don't need to call it yourself. It's dispatched on `Dispatchers.IO`
internally, off the caller's thread.

The SDK works with or without ad-tracking permission:

* If the Google Play Services advertising ID is available and the user has **not** limited ad
  tracking, `identifierType` resolves to `"GAID"` using the device's real advertising identifier.
* If the advertising ID is unavailable (no Play Services, the ID comes back all-zeros, or the user
  has opted out via **"Delete advertising ID"** / **"Limit ad tracking"** in Android Settings),
  `identifierType` falls back to `"OTHER"` with a random UUID generated once and persisted
  locally (`SharedPreferences`) — campaigns will be non-personalized in this case.

Offerwall integrations perform better when a real GAID is available — for the best attribution
accuracy, call `loginUser()` as early as your flow reasonably allows (see
[Initialization](initialization.md)).
