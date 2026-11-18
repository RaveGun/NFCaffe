# Maintaining NFCoffee

A one-page guide organised by **when** you need to do something, not by topic.
If you read one file a year, read this one.

---

## The one thing that forces action

**Every 31 August**, Google Play raises the minimum `targetSdk`. If the app is
below it:

- you can't upload any update until you raise it, and
- new users on newer Android versions stop seeing the app.

Extensions to 1 November are sometimes offered in Play Console. Nothing else
about this app expires — the keys are valid until 2054, the CSV format is
plain text, and the database schema hasn't changed since 2017.

Current baseline (November 2026): `compileSdk 37`, `targetSdk 36`.
The next forced bump is **targetSdk 37, by 31 August 2027**.

---

## Annual check — about 30 minutes, first week of August

1. Open Play Console → the app → **Policy status** / **Publishing overview**.
   Fix anything listed there first; it usually has a deadline.
2. Check the current requirement:
   <https://developer.android.com/google/play/requirements/target-sdk>
3. Compare with `app/build.gradle` → `targetSdk`.
4. **If the requirement is higher**, do the "targetSdk bump" below.
5. Glance at the pre-review warnings after the next upload (they are advisory,
   not blockers).
6. Nothing else is time-sensitive.

### targetSdk bump

```bash
# 1. edit app/build.gradle
#    compileSdk = <newest installed>      targetSdk = <required>
#    versionCode += 1                     versionName = "5.3"

# 2. build and test
./gradlew test assembleDebug lintDebug bundleRelease

# 3. run the four real-device flows (see below)

# 4. release it (see "Cutting a release")
```

If the new target introduces permissions or behaviour changes, they will show
up in the Play pre-review checks and in Android's behaviour-change list for
that version. Two are already queued here: `ACCESS_LOCAL_NETWORK` and the
edge-to-edge items in `docs/UPGRADE-5.2.md`.

---

## Cutting a release — the exact steps

```bash
cd /run/media/cox/newhome/backup/Ubuntu/cox/Development/NFCaffe

# 1. signing must point at the upload key (never committed)
grep storeFile keystore.properties
#    expect: /home/cox/AndroidKeysStore/nfcoffee-upload.p12

# 2. build everything and check the linter
./gradlew test assembleDebug lintDebug bundleRelease

# 3. prove which key signed the bundle
/var/lib/flatpak/app/com.google.AndroidStudio/x86_64/stable/active/files/extra/jbr/bin/keytool \
  -printcert -jarfile app/build/outputs/bundle/release/app-release.aab | grep SHA256
#    expect: 86:C7:66:52:16:22:78:B1:…:6B:6B:43
```

Then in Play Console:

4. **Test and release → Production → Create new release**.
5. Upload `app/build/outputs/bundle/release/app-release.aab`.
6. Write the *What's new* text (500 characters).
7. **Save → Review release → Start rollout to Production**.
8. Review takes hours to a couple of days.

Optional but useful before a production rollout: **Test and release → Internal
app sharing**, upload the same AAB, install from the link. It is the exact
artifact Play would serve, with no review.

After it is live: install from Play on a phone and run the four flows below.

---

## The four flows — test these before every release

1. **Transfer** — put the reader in transfer mode, press the cup, confirm the
   Wi-Fi dialog, check that counters land and the status panel reports it.
2. **NFC** — Manage IDs → pencil → Scan ID → tap a card → Edit ID opens with
   the card data.
3. **Edit** — change a name, Save, confirm the list updates.
4. **Export / import** — Export to a folder, open the CSV on the PC; then
   Import it back (Paste or Open file) and confirm the counters are right.

Also worth a glance: Delete counters sets everything to zero, and long-press
selects a row for deletion.

---

## When something goes wrong

### The build fails

| Message | Cause |
|---|---|
| `keystore.properties not found` | the file is missing — see `docs/SIGNING.md` |
| `Keystore was tampered with, or password was incorrect` | `storePassword` is wrong |
| `Cannot recover key` | `keyPassword` differs; PKCS12 needs both identical |
| `Failed to read key key0 from store …` | `storeFile` points at the wrong keystore |
| `Dependency … requires compileSdk 37 or later` | bump `compileSdk` |

### Play rejects the upload

| Message | Cause |
|---|---|
| *app bundle was signed with the wrong key* | `keystore.properties` points at the old key |
| *version code … has already been used* | bump `versionCode` |
| *must target API level …* | bump `targetSdk` |

### Something is lost

- **Upload key** → Play Console → **Protected with Play → Play Store
  protection → Manage Play app signing → Upload key certificate → Request
  reset**. The app keeps its identity; users notice nothing. Takes a couple of
  days.
- **Laptop** → the keystore is on the NAS (`only4me/AndroidKeysStore/`) and on
  the USB stick in the safe; the passphrase is in Bitwarden with a paper copy
  in the safe.
- **Phone data** → the app stores everything locally. Exports in `Download/`
  survive an uninstall; the database does not.

---

## Where everything lives

| What | Where |
|---|---|
| Project | `/run/media/cox/newhome/backup/Ubuntu/cox/Development/NFCaffe` |
| GitHub | <https://github.com/RaveGun/NFCaffe> (public, branch `master`) |
| Upload key | `~/AndroidKeysStore/nfcoffee-upload.p12` (PKCS12, alias `key0`) |
| Key backups | NAS `only4me/AndroidKeysStore/`, USB stick in the safe |
| Store screenshots + 512 px icon | `~/NFCoffee-store/` |
| Signing details | `docs/SIGNING.md` |
| What changed in 5.2 | `docs/UPGRADE-5.2.md` |
| Lint report | `app/build/reports/lint-results-debug.html` |
| R8 mapping (for deobfuscating crash reports) | `app/build/outputs/mapping/release/mapping.txt` |

Build from a terminal with the JDK bundled in Android Studio:

```bash
export JAVA_HOME=/var/lib/flatpak/app/com.google.AndroidStudio/x86_64/stable/active/files/extra/jbr
export PATH=$JAVA_HOME/bin:$PATH
```

---

## Every couple of years (optional)

- Update Android Gradle Plugin, Gradle, and AndroidX/Material to current
  versions, then re-run the four flows. The versions in
  `gradle/libs.versions.toml` are from November 2026.
- Skim `app/build/reports/lint-results-debug.html` for new warnings.

Known deferred work is listed at the end of `docs/UPGRADE-5.2.md`:
`EdgeToEdge.enable()` in `BaseActivity`, `ACCESS_LOCAL_NETWORK` at targetSdk 37,
a ViewModel for the transfer log, and unit tests for the card-code conversion.

**Not applicable:** the 16 KB page-size requirement (February 2027) only affects
apps with native code. This app is pure Java.

---

## Calendar reminders to set

| When | What |
|---|---|
| 1 August, yearly | Check the Play targetSdk requirement and release if needed |
| 1 August 2027 | Expect `targetSdk 37` to be required; also add `ACCESS_LOCAL_NETWORK` |
| After any key change | Re-check the fingerprint in Play Console and in `docs/SIGNING.md` |
