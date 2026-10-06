# Device Data

`loginUser()` sends a `deviceData` object on every call, collected via native Android platform
APIs:

| Field | Source |
|---|---|
| `deviceId`, `product`, `androidId` | `Settings.Secure.ANDROID_ID` |
| `brand`, `model`, `manufacturer`, `fingerprint`, `hardware`, `host`, `tags`, `type`, `codename` | `android.os.Build.*` |
| `baseOs` | `"Android"` |
| `releaseVersion`, `androidVersion` | `Build.VERSION.RELEASE` |
| `androidApiLevel` (number), `androidSdkInt` (string) | `Build.VERSION.SDK_INT` |
| `build`, `version` | Your app's `versionCode` / `versionName` |
| `package` | Your app's package name |
| `installerStore`, `installerPackageName` | Installer package name (`"unknown"` if sideloaded) |
| `device` | `"phone"` or `"tablet"` |
| `virtual` | Emulator detection |
| `rooted` | Root detection ([RootBeer](https://github.com/scottyab/rootbeer)) |
| `carrierName`, `mcc`, `mnc`, `mccMnc`, `countryIso`, `isRoaming`, `simOperatorName`, `simOperator`, `simCountryIso`, `phoneType` | `TelephonyManager` (`"Unknown"` when unavailable), no `READ_PHONE_STATE` needed |
| `supportedAbis`, `supported32BitAbis`, `supported64BitAbis` | `Build.SUPPORTED_*ABIS`, comma-separated |
| `cpuType` | Primary ABI |
| `cpuCores` | Processor count (string) |
| `cpuModel`, `cpuHardware` | `Build.SOC_MODEL` (API 31+, else `"Unknown"`) / `Build.HARDWARE` |
| `osArch` | `os.arch` system property |
| `totalMemory` | **Total device storage** (not RAM), decimal GB with 2 decimals, e.g. `128.03`. Matches the other TyrAds SDKs |
| `maxMemory`, `freeMemory` | App heap in MB (strings) |
| `screenWidth`, `screenHeight` | Full screen size in dp |
| `screenDensity` | Display density |
| `connectionType` | `wifi` / `cellular` / `ethernet` / `bluetooth` / `vpn` / `none` / `unknown` |
| `isVpnActive` | `true` whenever a VPN is active, including one running over wifi/cellular |
| `networkSpeed` | Downstream bandwidth, e.g. `"3750 KB/s"` |
| `osLang` | Device locale, `{lang}-{COUNTRY}` (e.g. `en-US`) |
| `timeZone` | IANA zone ID (e.g. `Asia/Jakarta`) |
| `timeZoneOffset` | Current UTC offset in minutes |
| `systemTime` | Current time, epoch milliseconds |
| `gpu` | GPU / SoC name |
| `bluetooth` | `BLE_SUPPORTED` / `CLASSIC_SUPPORTED` / `NOT_SUPPORTED` |
| `touchSupport` | `MULTITOUCH_JAZZHAND` / `MULTITOUCH_DISTINCT` / `MULTITOUCH` / `SINGLE_TOUCH` / `NOT_SUPPORTED` |
| `googleAppSetID` | Google App Set ID (`"Unknown"` if unavailable) |
| `deviceUpTime`, `deviceBootTime` | Hours since boot / boot timestamp |
| `buildSign` | SHA-256 of the APK signing certificate(s), uppercase hex |
| `serialNumber` | `Build.getSerial()` (`"unknown"` without permission) |
| `deviceAge` | Estimated device manufacture date, epoch milliseconds |
| `apiVersion`, `sdkVersion` | This SDK's version constants |
| `sdkPlatform` | `"Android-userbase"`, so the backend can tell this SDK apart from the full Android SDK |

### Not replicated

`keyboardNumEvents`/`Score`, `clipboardNumEvents`/`Score`, and `click`/`mouse`/`touchNumEvents`
are always sent as `0`. Per-app behavioral telemetry (keyboard/clipboard/touch event counters)
isn't tracked by this SDK.
