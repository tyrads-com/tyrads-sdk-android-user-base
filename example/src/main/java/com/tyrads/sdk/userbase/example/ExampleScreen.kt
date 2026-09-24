package com.tyrads.sdk.userbase.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyrads.sdk.userbase.TyradsUserBase
import com.tyrads.sdk.userbase.config.TyradsConfig
import com.tyrads.sdk.userbase.config.TyradsEnvironment
import com.tyrads.sdk.userbase.models.Campaign
import com.tyrads.sdk.userbase.models.ActivatedCampaignsResponse
import com.tyrads.sdk.userbase.models.TyradsInitOptions
import com.tyrads.sdk.userbase.models.TyradsInitResponse
import com.tyrads.sdk.userbase.models.TyradsOfferwallUrlOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

private const val PREFS_NAME = "tyrads_ub_example_credentials"

private class SavedCredentials(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): Map<String, String> = mapOf(
        "apiKey" to (prefs.getString("apiKey", "") ?: ""),
        "apiSecret" to (prefs.getString("apiSecret", "") ?: ""),
        "encKey" to (prefs.getString("encKey", "") ?: ""),
        "userId" to (prefs.getString("userId", "test_user_1") ?: "test_user_1"),
        "pushToken" to (prefs.getString("pushToken", "") ?: ""),
        "environment" to (prefs.getString("environment", "production") ?: "production"),
    )

    fun save(apiKey: String, apiSecret: String, encKey: String, userId: String, pushToken: String, environment: String) {
        prefs.edit()
            .putString("apiKey", apiKey)
            .putString("apiSecret", apiSecret)
            .putString("encKey", encKey)
            .putString("userId", userId)
            .putString("pushToken", pushToken)
            .putString("environment", environment)
            .apply()
    }
}

@Composable
fun ExampleScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentials = remember { SavedCredentials(context) }

    var environment by remember { mutableStateOf("production") }
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var encKey by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("test_user_1") }
    var pushToken by remember { mutableStateOf("") }
    var campaignId by remember { mutableStateOf("") }
    var offerwallRoute by remember { mutableStateOf("") }

    var pushTokenStatus by remember { mutableStateOf(PushTokenStatus.IDLE) }
    var isReady by remember { mutableStateOf(false) }
    var loadingKey by remember { mutableStateOf<String?>(null) }

    var sessionInfo by remember { mutableStateOf<JsonResult?>(null) }
    var recommendation by remember { mutableStateOf<JsonResult?>(null) }
    var detail by remember { mutableStateOf<JsonResult?>(null) }
    var activated by remember { mutableStateOf<JsonResult?>(null) }
    var activation by remember { mutableStateOf<JsonResult?>(null) }
    var offerwallUrl by remember { mutableStateOf<JsonResult?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* fire-and-forget — a real FCM token can still be fetched without this permission */ }

    LaunchedEffect(Unit) {
        val saved = credentials.load()
        apiKey = saved.getValue("apiKey")
        apiSecret = saved.getValue("apiSecret")
        encKey = saved.getValue("encKey")
        userId = saved.getValue("userId")
        pushToken = saved.getValue("pushToken")
        environment = saved.getValue("environment")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        pushTokenStatus = PushTokenStatus.FETCHING
        val token = fetchFcmToken()
        if (token != null) {
            pushToken = token
            pushTokenStatus = PushTokenStatus.FETCHED
        } else {
            pushTokenStatus = PushTokenStatus.UNAVAILABLE
        }
    }

    fun runAction(key: String, onResult: (JsonResult) -> Unit, action: suspend CoroutineScope.() -> JsonResult) {
        loadingKey = key
        scope.launch {
            val result = try {
                action()
            } catch (t: Throwable) {
                formatError(t)
            }
            onResult(result)
            loadingKey = null
        }
    }

    fun handleInitAndLogin() = runAction("session", { sessionInfo = it }) {
        credentials.save(apiKey, apiSecret, encKey, userId, pushToken, environment)
        TyradsConfig.setEnvironment(if (environment == "staging") TyradsEnvironment.STAGING else TyradsEnvironment.PRODUCTION)
        TyradsUserBase.init(context, apiKey.trim(), apiSecret.trim(), encKey.trim().ifBlank { null }, debugMode = true)
        val result = TyradsUserBase.loginUser(
            userId.trim(),
            TyradsInitOptions(devicePushToken = pushToken.trim().ifBlank { null }),
        )
        isReady = true
        formatSuccess(TyradsInitResponse.serializer(), result)
    }

    fun handleGetRecommendation() = runAction("recommendation", { recommendation = it }) {
        formatSuccess(ListSerializer(Campaign.serializer()), TyradsUserBase.getCampaigns())
    }

    fun handleGetDetail() = runAction("detail", { detail = it }) {
        formatSuccess(Campaign.serializer(), TyradsUserBase.getCampaignDetail(campaignId.trim()))
    }

    fun handleGetActivated() = runAction("activated", { activated = it }) {
        formatSuccess(ActivatedCampaignsResponse.serializer(), TyradsUserBase.getActivatedCampaigns())
    }

    fun handleActivate() = runAction("activate", { activation = it }) {
        formatSuccessJson(TyradsUserBase.activateCampaign(campaignId.trim()))
    }

    fun handleGetOfferwallUrl() = runAction("offerwallUrl", { offerwallUrl = it }) {
        formatSuccessUrl(
            TyradsUserBase.getOfferwallUrl(
                TyradsOfferwallUrlOptions(
                    campaignId = campaignId.trim().ifBlank { null },
                    route = offerwallRoute.trim().ifBlank { null },
                ),
            ),
        )
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Tyrads User-Base", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ExampleColors.titleText)
            Text(
                "Example and demo App",
                fontSize = 13.sp,
                color = ExampleColors.subtitleText,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )
        }

        item {
            SectionCard("1. Credentials & Session") {
                Text("Environment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ExampleColors.inputLabel)
                EnvToggleRow(active = environment, enabled = !isReady) { environment = it }
                if (isReady) {
                    Hint("Environment locked after Init & Login — restart app to switch")
                }
                LabeledInput("API Key", apiKey, { apiKey = it }, "API Key")
                LabeledInput("API Secret", apiSecret, { apiSecret = it }, "API Secret")
                LabeledInput("Enc Key (optional)", encKey, { encKey = it }, "32-char encryption key")
                LabeledInput("User ID", userId, { userId = it }, "User ID")
                LabeledInput(
                    label = "Device Push Token (optional)" + if (pushTokenStatus == PushTokenStatus.FETCHING) " — fetching…" else "",
                    value = pushToken,
                    onValueChange = { pushToken = it },
                    placeholder = if (pushTokenStatus == PushTokenStatus.UNAVAILABLE) {
                        "Auto-fetch failed — paste one manually"
                    } else {
                        "FCM registration token"
                    },
                )

                ActionButton("Init & Login", ::handleInitAndLogin, loading = loadingKey == "session")
                Row(modifier = Modifier.padding(top = 10.dp)) {
                    if (isReady) StatusPill("Session ready")
                    if (isReady && pushToken.isNotBlank()) {
                        Spacer(Modifier.padding(start = 8.dp))
                        StatusPill("Push token relayed")
                    }
                }
                JsonOutput(sessionInfo)
            }
        }

        item {
            SectionCard("2. Campaign Recommendation") {
                Hint("GET campaigns?mode=userbase")
                ActionButton("Get Campaign Recommendation", ::handleGetRecommendation, loading = loadingKey == "recommendation", enabled = isReady)
                JsonOutput(recommendation)
            }
        }

        item {
            SectionCard("3. Campaign Detail") {
                Hint("GET campaigns/:id?mode=userbase")
                LabeledInput("Campaign ID", campaignId, { campaignId = it }, "e.g. 12345", KeyboardType.Number)
                ActionButton("Get Campaign Detail", ::handleGetDetail, loading = loadingKey == "detail", enabled = isReady && campaignId.isNotBlank())
                JsonOutput(detail)
            }
        }

        item {
            SectionCard("4. Activated Campaigns") {
                Hint("GET campaigns/activated")
                ActionButton("Get Activated Campaigns", ::handleGetActivated, loading = loadingKey == "activated", enabled = isReady)
                JsonOutput(activated)
            }
        }

        item {
            SectionCard("5. Activate Campaign") {
                Hint("POST campaigns/:id/activate — uses Campaign ID above")
                ActionButton(
                    "Activate Campaign",
                    ::handleActivate,
                    loading = loadingKey == "activate",
                    enabled = isReady && campaignId.isNotBlank(),
                    danger = true,
                )
                JsonOutput(activation)
            }
        }

        item {
            SectionCard("6. Offerwall URL (no webview shown)") {
                Hint("Same URL native showOffers() opens in a webview — returned as JSON instead of rendered")
                LabeledInput("Route (optional)", offerwallRoute, { offerwallRoute = it }, "e.g. offers, activeOffers")
                ActionButton("Get Offerwall URL", ::handleGetOfferwallUrl, loading = loadingKey == "offerwallUrl", enabled = isReady)
                JsonOutput(offerwallUrl)
            }
        }
    }
}
