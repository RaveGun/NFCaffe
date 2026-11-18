# NFCoffee

Android app that reads coffee counters from an NFC card reader / coffee machine
over Wi-Fi, stores them per card in a local database, and exports/imports the
list as CSV.

## Requirements

- Android Studio (the Flatpak build works)
- Android SDK Platform 37 + Build-Tools 36.0.0 (SDK Manager)
- JDK 17 or newer (the JBR bundled with Android Studio is fine)

`local.properties` must point at your SDK:

```properties
sdk.dir=/home/cox/Android/Sdk
```

## Build

From a terminal, Gradle needs a JDK. The JDK inside the Android Studio Flatpak
works directly:

```bash
export JAVA_HOME=/var/lib/flatpak/app/com.google.AndroidStudio/x86_64/stable/active/files/extra/jbr
export PATH=$JAVA_HOME/bin:$PATH

./gradlew assembleDebug      # debug APK  -> app/build/outputs/apk/debug/
./gradlew bundleRelease      # signed AAB -> app/build/outputs/bundle/release/
./gradlew lintDebug          # lint report -> app/build/reports/lint-results-debug.html
```

Release builds need `keystore.properties` in the project root (git-ignored).
Without it, `bundleRelease` fails on purpose instead of producing an unsigned
bundle. See [docs/SIGNING.md](docs/SIGNING.md).

Install on a device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Documentation

- [docs/SIGNING.md](docs/SIGNING.md) — signing key location, verification,
  creating a new upload key, Play App Signing.
- [docs/UPGRADE-5.2.md](docs/UPGRADE-5.2.md) — what changed in the 2026
  platform upgrade, what was tested where, and what is still to do.
- [docs/MAINTENANCE.md](docs/MAINTENANCE.md) — the yearly checklist: when Play
  raises the target SDK, how to cut a release, what to do when something breaks.

## Project layout

```
app/src/main/java/ro/dsgmedia/cox/nfcoffee/
  MainActivity.java      drawer, "Get Data" button, background task runner
  WifiConnect.java       joins the NFCoffee access point
  getTCPData.java        raw TCP client for the reader
  TCPTransfer.java       parses the reader protocol, updates the counters
  ManageIDs.java         list of cards, delete, export/import shortcuts
  EditID.java            edit one card
  NFCReadCard.java       NFC reader-mode screen
  ShareData.java         CSV export (SAF) / import (pasted text)
  SqlHelper_NFCIDs.java  SQLiteOpenHelper, schema v1
  WindowInsetsUtil.java  edge-to-edge insets helper
```
