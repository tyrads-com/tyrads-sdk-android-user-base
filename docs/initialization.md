# Initialization

This SDK is headless: there's no offerwall, no premium widget, no in-app notification popups.
Every method returns typed data (or throws) so you can build your own UI (or none at all) on top
of it. The integration flow is simple: `init` → `loginUser` → everything else.

Every network-backed method has a `suspend fun` (call from a coroutine scope) and a Java-friendly
callback overload (`TyradsCallback` / `TyradsResultCallback<T>` / `TyradsLoginCallback`).
Kotlin snippets below use the suspend form.

### 1. Initialize

Call `init` once, as early as possible in your app's lifecycle. It only stores credentials,
restores a previous session from disk if present, and prepares the HTTP client. No network call
happens here.

```kotlin
import com.tyrads.sdk.userbase.TyradsUserBase

TyradsUserBase.init(
    context = applicationContext,
    apiKey = "YOUR_API_KEY",
    apiSecret = "YOUR_API_SECRET",
    encKey = "YOUR_32_BYTE_ENC_KEY", // optional, enables AES-256-GCM secure mode
    debugMode = false, // optional, true logs full request/response traffic via Logcat
    interceptors = emptyList(), // optional, extra OkHttp interceptors (e.g. ChuckerInterceptor)
)
```

`interceptors` are added to the SDK's own OkHttp client, after its auth headers, so a network
inspector like [Chucker](https://github.com/ChuckerTeam/chucker) sees the exact requests the SDK
sends. Use it in debug/QA builds only.

A previous login is restored automatically on relaunch (`init()` reads any persisted
`userId`/`token` back into `getSession()`), so no explicit `restore()` call is needed.

### 2. Login

Logging in is mandatory and must be called after `init`. This is the call that actually hits the
network (`POST initialize`) and opens the session. The response's `accountInfo.publisherUserId`
becomes the `X-User-ID` header on every call after this.

```kotlin
val session = TyradsUserBase.loginUser("my_unique_user_123")
```

#### Advanced login (attribution + user info)

```kotlin
import com.tyrads.sdk.userbase.models.TyradsInitOptions
import com.tyrads.sdk.userbase.models.TyradsMediaSourceInfo
import com.tyrads.sdk.userbase.models.TyradsUserInfo

TyradsUserBase.loginUser(
    userId = "my_unique_user_123",
    options = TyradsInitOptions(
        engagementId = 123,
        placementId = 456,
        mediaSourceInfo = TyradsMediaSourceInfo(
            mediaSourceName = "Facebook",
            mediaCampaignName = "Summer2023Promo",
            mediaSourceId = "FB001",
            mediaSubSourceId = "FB001_Stories",
            incentivized = false,
            mediaAdsetName = "YoungAdults25-34",
            mediaAdsetId = "AD001",
            mediaCreativeName = "SummerSale_Video",
            mediaCreativeId = "CR001",
            sub1 = "ReferralCode123",
            sub2 = "OrganicInstall",
            sub3 = "HighValueUser",
            sub4 = "FirstTimeUser",
            sub5 = "AndroidDevice",
        ),
        userInfo = TyradsUserInfo(
            email = "user@example.com",
            phoneNumber = "001234567890", // replace leading + with 00
            userGroup = "High purchase user",
            age = 28,
            gender = 1, // 1 = male, 2 = female
        ),
    ),
)
```

#### `TyradsInitOptions` reference

| Parameter | Type | Description |
|---|---|---|
| `engagementId` | `Int?` | Identifies the engagement placement. |
| `placementId` | `Int?` | Differentiates coin placements within your app. Also used by `getOfferwallUrl()`. |
| `mediaSourceInfo` | `TyradsMediaSourceInfo?` | Attribution data. |
| `userInfo` | `TyradsUserInfo?` | User profile metadata. |

Note: `sub1`, `sub2`, `sub5` are not returned on postback; `sub3` and `sub4` are.

#### `loginUser` response (`TyradsInitResponse`)

```kotlin
@Serializable
data class TyradsInitResponse(
    val code: Int,
    val message: String,
    val timestamp: Long,
    val responseTime: Double, // fractional milliseconds, not an integer
    val data: TyradsInitData,
)

data class TyradsInitData(
    val newRegisteredUser: Boolean,
    val newRegisteredDevice: Boolean,
    val accountInfo: TyradsAccountInfo, // { id: Long, publisherUserId: String }
    val appInfo: TyradsAppInfo,         // { headerColor, mainColor, premiumColor: String }
    val token: String, // session token, stored locally and returned by getSession()
)
```

`data.accountInfo.publisherUserId` is what subsequently gets sent as `X-User-ID`; `data.token` is
stored locally and returned by `getSession()`. `appInfo`'s colors are meant for UI theming, safe
to ignore if you don't need them.

### 3. Campaigns

```kotlin
val campaigns = TyradsUserBase.getCampaigns()
// -> GET campaigns?mode=userbase, recommended/targeted campaigns

val detail = TyradsUserBase.getCampaignDetail(campaigns.first().campaignId.toString())
// -> GET campaigns/:id?mode=userbase, same Campaign shape, single object

val activated = TyradsUserBase.getActivatedCampaigns()
// -> GET campaigns/activated

val summary = TyradsUserBase.getActivatedSummary()
// -> GET campaigns/activated/summary -> Int
```

#### `Campaign` shape (`getCampaigns()` and `getCampaignDetail()` both return this)

`campaigns/:id` under `mode=userbase` returns the exact same flat shape as the list, as a single
object (not wrapped in an array). There is no separate, richer "detail" schema in this mode.

```kotlin
@Serializable
data class Campaign(
    val campaignId: Int,
    val tracking: CampaignTracking,          // { impressionUrl, clickUrl, s2sClickUrl }
    val packageName: String,
    val os: String,
    val title: String,
    val thumbnail: String?,
    val creativeUrl: String?,
    val campaignDescription: String?,
    val installedOn: String?,
    val activatedOn: String?,
    val uninstalledOn: String?,
    val expiredOn: String?,
    val expiredInSeconds: Long?,
    val currencies: List<AvailableCurrency>,  // { currencyId, currencyIcon, currencyName }
    val activeCurrencyId: Int?,
    val payoutSummary: Map<String, PayoutSummary>, // keyed by currencyId
    val earnedPayout: Map<String, JsonElement>,
    val stage: String?,
    val engagements: List<JsonElement>,
    val events: List<CampaignEvent>,
)
```

`CampaignEvent` fields: `appEventId`, `identifier`, `eventName`, `eventDescription`,
`allowDuplicateEvents`, `payoutInfo` (`Map<String, CampaignEventPayout>`, just
`{ payoutAmountConverted }`), `rewardedOn`, `conversionStatus`, `lockEventRule`/`hideEventRule`
(arrays), `shorterMaxTimeRule`, `isTicketSubmitted`, `ticketStatus`,
`ticketUrl`, `ticketRejectReason`, `ticketRejectionCode`, `count`, `limit`, `maxTime`,
`maxTimeMetric`, `maxTimeRemainSeconds`, `enforceMaxTimeCompletion`, `rewardingExpiredOn`,
`rewardingExpiredInSeconds`, `type` (e.g. `"Playable"`).

#### `ActivatedCampaign` shape (`getActivatedCampaigns()`, inside each group's `campaigns[]`)

`campaignId`, `campaignName`, `campaignDescription`, `campaignType`, `campaignPremium`,
`validity` (`isRetryDownload`/`isActivated`/`isOldUser`/`expiredOn`/`expiredInSeconds`/
`isInstalled`/`activeCurrencyId`/`capReached`), `availableCurrencies`, `campaignStatus`, `group`,
`stage`, `eventSummary` (`playableEventCount*`/`microchargeEventCount*` `Available`/`Completed`/
`Total`), `limitedTimeEvents[]`, `shorterMaxTimeEvents[]`.

### 4. Activate a Campaign

```kotlin
val result = TyradsUserBase.activateCampaign(campaigns.first().campaignId.toString())
// -> POST campaigns/:id/activate, e.g. { "code": 200, "data": { "isCampaignActivated": true }, ... }
```

Returns the raw JSON body (`kotlinx.serialization.json.JsonElement`). The shape is a thin
passthrough, not modeled as a data class. The response includes the campaign's
`tracking.clickUrl` (from the `Campaign` you already fetched). Open it yourself
(`Intent(Intent.ACTION_VIEW, Uri.parse(url))`, a `CustomTabsIntent`, or an in-app `WebView`) if you
want to send the user onward; this SDK never opens anything on its own.

### 5. Offerwall URL (no webview shown)

Builds the offerwall URL and hands it back as a plain `String` instead of rendering it. Sync, no
network call:

```kotlin
import com.tyrads.sdk.userbase.constants.TyradsDeepRoutes
import com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions

val url = TyradsUserBase.getOfferwallUrl(
    TyradsOfferwallUrlOptions(
        route = TyradsDeepRoutes.SETTINGS, // optional, omit for the main offerwall
        campaignId = "42",                 // optional
    ),
)
```

See [Deeplinking Routes](initialization/deeplinking-routes.md) for the full route reference.
Throws `IllegalStateException` if there's no active session (call `loginUser` first).

### 6. Currency Sale / Engagement Data

```kotlin
val engagement = TyradsUserBase.getEngagement()
// -> GET account/engagement -> CurrencySales?
```

`CurrencySales?` is `null` whenever there's no currency sale currently active. When non-null:
`{ name, multiplier, bannerUrl, dateStart, dateEnd, remainingTimeSeconds }`. See also
`limitedTimeEvents` on `getActivatedCampaigns()`.

### 7. Manual Tracking

```kotlin
import com.tyrads.sdk.userbase.constants.TyradsActivity

TyradsUserBase.trackActivity(TyradsActivity.OPENED)
TyradsUserBase.trackActivity(TyradsActivity.CLOSED)
TyradsUserBase.trackActivity("any-custom-string")
```

`TyradsActivity` constants: `INITIALIZED`, `PROFILE_UPDATED`, `OPENED`,
`TARGETED_CAMPAIGN_SHOWN`, `TARGETED_CAMPAIGN_DETAIL_SHOWN`, `CAMPAIGN_ACTIVATED`,
`CAMPAIGN_ACTIVATED_RETRY`, `SUPPORT_TICKET_SHOWN`, `CLOSED`, or pass any custom string.
`trackActivity(TyradsActivity.INITIALIZED)` is already fired automatically on a successful
`loginUser()` call. Nothing else is tracked automatically, so call this yourself at
whatever points matter for your integration.

### 8. Session & Logout

```kotlin
TyradsUserBase.getSession()
// -> TyradsSession(userId, token, isLoginSuccessful, currentLanguage), local state, no network

TyradsUserBase.logoutUser()
// clears the local session (userId/token). Credentials from init() are kept. Call
// loginUser() again to re-authenticate.
```

Both are synchronous, no coroutine/callback needed.

### 9. Language

```kotlin
TyradsUserBase.changeLanguage("id-ID") // Indonesian
```

Sets the `lang` query param used by campaign endpoints. Format is `{lang}-{COUNTRY}` (e.g.
`en-US`, `pt-BR`, `id-ID`). Defaults to whatever the device's locale resolves to (`en-US` on
error). There's no bundled UI to translate, so this only affects what language campaign content
comes back in.

## Notes

* `init` must be called before any other SDK method.
* `loginUser` must be called before any campaign/offerwall/tracking method.
* Every network-backed method is a `suspend fun` (call from a coroutine scope), or use the
  matching `TyradsCallback`/`TyradsResultCallback<T>`/`TyradsLoginCallback` overload from Java.
  `getSession()` and `changeLanguage()` are synchronous local-state operations either way.
