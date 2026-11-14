# NFCoffee 5.2 — platform upgrade notes

This file records what changed in the November 2026 modernisation pass, what
was verified on a device, and what still has to be tested on real hardware.

## Why

The last published build was `versionCode 6` / `versionName 5.1`, targeting
**API 33**. Google Play has required **API 36** for every upload since
31 August 2026, and an app below API 35 stopped being shown to new users on
newer Android versions in August 2025. In other words: 5.1 could not be updated
and was effectively invisible to anyone on Android 14 or newer.

## Toolchain

| | Before | Now |
|---|---|---|
| Android Gradle Plugin | 8.2.0 | 9.5.1 |
| Gradle | 8.2 | 9.9.1 |
| compileSdk | 33 | 37 (Android 17) |
| targetSdk | 33 | **36 (Android 16)** |
| minSdk | 30 | 30 (unchanged) |
| Java | implicit | 17 (source/target) |
| Version | 6 / "5.1" | **7 / "5.2"** |
| R8 | off | **on** (+ resource shrinking) |

`targetSdk` is deliberately one level below `compileSdk`: we get the newest
libraries, but do not opt the app into Android 17 behaviour changes before it
has been tested there. Raise it to 37 in the next cycle.

Release size: the signed AAB dropped from 5.09 MB to 1.88 MB thanks to R8.

## Signing

`app/build.gradle` no longer contains any password. It reads
`keystore.properties` (git-ignored). See `docs/SIGNING.md` for the key
location, how to verify it, and how to create a replacement upload key.

## Behaviour that was intentionally preserved

- Same package id, same database schema and file name (`NFCoffee`), so an
  in-place upgrade keeps all card data.
- Same screens, same buttons, same labels, same CSV format and header line.
- Same counter arithmetic on import (values are *added* to the local counter).
- Portrait orientation is still enforced for now (see "Still to do").
- No runtime permission prompts at all any more (see below), so the first-run
  experience actually gains a step rather than losing one.

## Functional fixes

| Area | Before | After |
|---|---|---|
| CSV export | Wrote to `/storage/emulated/0/Documents` with `WRITE_EXTERNAL_STORAGE`, which cannot work on API 30+. **Export was broken on every supported device.** | Uses the Storage Access Framework. The system asks where to save (suggested name `export_YYYYMMDD.csv`). Verified working. |
| Copy / Paste buttons | Empty stubs | Implemented (clipboard copy of the CSV, paste into the import box). |
| Wi-Fi connect | Polled `WifiManager.getConnectionInfo()` every 500 ms (deprecated API 31+, returns `unknown` without location permission) and **never released** the process-to-network binding, so the phone stayed on the coffee machine's Wi-Fi after a transfer. | Uses `NetworkCallback` with `requestNetwork(..., timeout)`, reads the IP from `LinkProperties`, and always unbinds in a `finally` block. |
| NFC scan | `enableForegroundDispatch` + `FLAG_MUTABLE` PendingIntent | `enableReaderMode` with A/B/F/V + `SKIP_NDEF_CHECK`. |
| SQL | String-concatenated `WHERE CODENAME='" + value + "'` in 8 places (a name with an apostrophe would break it). | Parameterised `?` / `whereArgs` everywhere. |
| Counter column | `COFF` written as a String into an INT column | Written as an Integer, parsed defensively. |
| Background failure | `throw new RuntimeException(e)` inside the executor → app crash | Logged, callback still runs, button state resets. |
| Number formatting | `String.format` / `toUpperCase` with the default locale | `Locale.ROOT`, so card codes cannot be mangled on a device with e.g. Arabic or Turkish locale. |
| EditID database | Closed in `onStop`, never reopened → crash when saving after returning from background | Reopened in `onStart`. |
| List selection | `setItemChecked(getSelectedItemPosition(), …)` could be `-1` | Uses the actual tapped position. |
| Delete entry | Reflective cast into a `HashMap` | Reads the `"Second Line"` value directly. |

## Platform changes handled

- **Edge-to-edge (mandatory from API 35)** — `EdgeToEdge.enable()` plus a
  `WindowInsetsUtil` helper that pads every screen by the system bars and
  display cutout. Removed the now-ignored `statusBarColor` /
  `windowDrawsSystemBarBackgrounds` theme attributes.
- **Predictive back** — `onBackPressed()` replaced by
  `OnBackPressedDispatcher`; `android:enableOnBackInvokedCallback="true"`.
- **Removed permissions** — `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
  and `WRITE_EXTERNAL_STORAGE` are gone; the modern APIs no longer need them.
  Remaining: `INTERNET`, `NFC`, `ACCESS_NETWORK_STATE`, `CHANGE_NETWORK_STATE`.
- **Manifest** — dropped the legacy `package=` attribute (superseded by
  `namespace`), dropped the `android.support.PARENT_ACTIVITY` meta-data,
  added `android:roundIcon`, fixed `ShareData`'s parent activity.

## Verified on the emulator (Medium Phone, Android 17 / API 37)

- App launches, no crash, no permission prompt.
- Toolbar and content correctly inset below the status bar and above the
  navigation bar.
- Drawer opens; a back press closes the drawer and stays in the app.
- Manage IDs list renders.
- Import of a pasted CSV line inserts a row (`Alice [12]`).
- Tapping a row opens Edit ID with all fields populated; Save updates and
  returns without error.
- Long-press → delete removes the row.
- "Delete counters" resets the counter to 0.
- Export: file picker opens with `export_20261114.csv`, the saved file in
  `Download/` contains exactly
  `NFCID,CODENAME,UNAME,EMAIL,COFF` + the data row.
- `assembleDebug`, `bundleRelease` (R8) and `lintDebug` all pass.

## MUST be tested on the real device

These cannot be exercised on an emulator:

1. **Wi-Fi + TCP transfer** (the main feature). The request now removes the
   `NET_CAPABILITY_INTERNET` requirement because the coffee machine has no
   upstream internet; confirm the system "connect to NFCoffee" dialog appears,
   the transfer runs, and the phone returns to normal Wi-Fi afterwards.
2. **NFC scan** — tap a card on the Manage IDs → Scan ID screen; it should
   open Edit ID with the card UID.
3. **Export on a real device** — pick a folder (e.g. Documents), confirm the
   file is readable as CSV.
4. Check the app in the launcher still looks right (icon unchanged).

## Still to do (not in this pass)

- **Phase 4 remainder (visual):** Material 3 + dark theme, adaptive icon,
  replacing `com.github.clans:fab`, removing the hardcoded 409dp × 640dp
  import box, hardcoded strings/colours, RTL paddings, and lifting the
  portrait lock (needs responsive layouts first, especially `activity_share`).
  Deliberately deferred because it needs visual review on a phone.
- **Phase 5:** ViewModel + Room + proper socket timeout + Kotlin.
- **Phase 6:** tests, Play Console Data safety update (permission set changed),
  rollout.

## Play Console checklist for the next upload

1. Upload `app/build/outputs/bundle/release/app-release.aab` (versionCode 7).
2. Update the **Data safety** form: location and "files and media" access are
   no longer requested; the app only accesses the network and NFC.
3. Confirm the upload key (`key0`, SHA-256 `A7:BF:D9:86:…:30:E8`) matches
   App integrity → App signing → Upload key certificate.
