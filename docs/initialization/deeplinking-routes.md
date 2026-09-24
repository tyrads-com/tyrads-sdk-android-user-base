# Deeplinking Routes

`getOfferwallUrl(TyradsOfferwallUrlOptions(route, campaignId, placementId))` accepts a set of
named route constants via `TyradsDeepRoutes`:

```kotlin
import com.tyrads.sdk.userbase.TyradsUserBase
import com.tyrads.sdk.userbase.constants.TyradsDeepRoutes
import com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions

val url = TyradsUserBase.getOfferwallUrl(TyradsOfferwallUrlOptions(route = TyradsDeepRoutes.OFFERS))
```

| Constant | Route | Description |
|---|---|---|
| `TyradsDeepRoutes.OFFERS` | `"offers"` | Main offerwall (default) |
| `TyradsDeepRoutes.ACTIVE_OFFERS` | `"activeOffers"` | User's active/installed offers |
| `TyradsDeepRoutes.SUPPORT` | `"support"` | Support page |
| `TyradsDeepRoutes.SETTINGS` | `"settings"` | Settings page |

`route` is a plain string param — passing any other value the backend recognizes works too, these
are just the ones shipped as named constants.
