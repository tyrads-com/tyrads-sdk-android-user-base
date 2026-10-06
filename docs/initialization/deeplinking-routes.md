# Deeplinking Routes

`getOfferwallUrl(TyradsOfferwallUrlOptions(route, campaignId, placementId))` accepts route constants
for jumping straight into a sub-page of the web offerwall, via `TyradsDeepRoutes`:

```kotlin
import com.tyrads.sdk.userbase.TyradsUserBase
import com.tyrads.sdk.userbase.constants.TyradsDeepRoutes
import com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions

val url = TyradsUserBase.getOfferwallUrl(TyradsOfferwallUrlOptions(route = TyradsDeepRoutes.SETTINGS))
```

| Constant | Route | Description |
|---|---|---|
| `TyradsDeepRoutes.SUPPORT` | `"support"` | Support page |
| `TyradsDeepRoutes.SETTINGS` | `"settings"` | Settings page |

Omit `route` entirely for the main offerwall. There's no bundled UI in this SDK, so
`offers`/`activeOffers` aren't meaningful deep-link targets the way they are for the native SDKs'
own webview.
