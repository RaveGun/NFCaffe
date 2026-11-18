# Release signing — NFCoffee

## Keys in play

| | **Previous upload key** (archived 2026-11-16) | **Upload key in use** (since 2026-11-16) |
|---|---|---|
| Keystore | `~/AndroidKeysStore/nfcoffee_key.jks` | `~/AndroidKeysStore/nfcoffee-upload.p12` |
| Format | JKS (legacy) | PKCS12 |
| Alias | `key0` | `key0` |
| Certificate SHA-256 | `A7:BF:D9:86:0F:DD:C2:6E:6E:96:FE:EF:F5:C3:F2:16:08:97:D4:CE:83:23:E8:2F:2B:79:11:C0:D4:37:30:E8` | `86:C7:66:52:16:22:78:B1:C9:91:13:3E:90:1B:A7:99:05:D3:79:AB:BE:B3:51:71:2D:3E:EB:B5:9E:6B:6B:43` |
| Certificate SHA-1 | `05:09:68:9F:CE:12:59:A7:D1:5A:E3:3C:36:78:D2:6F:E0:36:84:4D` | `19:D9:5E:CC:95:95:3D:2A:B4:7B:FB:7F:49:44:9A:00:97:F8:08:58` |
| Owner / validity | `O=DSGMedia.ro`, 2017-07-17 → 2116-06-23 | `CN=NFCoffee Upload, O=DSGMedia.ro, C=RO`, 2026-11-14 → 2054-04-01 |
| Key | RSA 2048 | RSA 4096 |

**Reset status:** requested 2026-11-14, activated 2026-11-16 14:11, **verified
2026-11-18**: Play Console's *Upload key* shows SHA-1
`19:D9:5E:CC:…:08:58`, and a release AAB signed with the new key reports the
same SHA-1 and SHA-256. The old key is no longer accepted for uploads.

`keystore.properties` now points at `nfcoffee-upload.p12`. One password only:
PKCS12 uses the same value for store and key, and `keytool` will not create
different ones.

### Switching (done 2026-11-18)

```properties
storeFile=/home/cox/AndroidKeysStore/nfcoffee-upload.p12
storePassword=<the new password>
keyAlias=key0
keyPassword=<the new password>
```

Then verify the bundle really was signed with it:

```bash
KT=$JAVA_HOME/bin/keytool
$KT -printcert -jarfile app/build/outputs/bundle/release/app-release.aab | grep SHA256
# must print 86:C7:66:52:...
```

The old keystore stays in the folder as an archive; it simply stops being
accepted for uploads.

## Where the copies live

| Copy | Location | Notes |
|---|---|---|
| Working copy | `~/AndroidKeysStore/nfcoffee-upload.p12` | mode 600, needed to build |
| NAS | `/media/coxshare/only4me/AndroidKeysStore/NFCoffee-signing/`<br>(`//192.168.1.50/share/coxshare`) | SHA-256 verified |
| USB stick | `NFCoffee-signing/` on the FAT32 stick | kept in the safe |
| Password | Bitwarden vault + paper copy in a sealed envelope in the safe | |

The NAS and USB copies also carry `README.txt` (fingerprints and restore
steps) and `archive-old-key/nfcoffee_key.jks` for the 2017 key.

The `.p12` is protected at rest by AES-256/PBES2 under the Bitwarden-generated
passphrase, so the FAT32 stick (no Unix permissions) and the CIFS share
(0770, LAN only) are acceptable storage without an extra encryption layer.

## Historical state (restored 2026-11-14)

| Item | Value |
|---|---|
| Keystore (canonical) | `~/AndroidKeysStore/nfcoffee_key.jks` |
| Keystore format | JKS (convert to PKCS12 when you rotate) |
| Alias used for release | `key0` |
| Certificate SHA-256 | `A7:BF:D9:86:0F:DD:C2:6E:6E:96:FE:EF:F5:C3:F2:16:08:97:D4:CE:83:23:E8:2F:2B:79:11:C0:D4:37:30:E8` |
| Certificate owner / validity | `O=DSGMedia.ro`, 2017-07-17 → 2116-06-23 |

Backups of the same file (identical SHA-256 `ee766c1c…`) still exist at:

- `~/../run/media/cox/newhome/backup/Ubuntu/cox/Development/nfcoffee_key.jks`
- `…/NFCaffe/ReleaseOutput/Old_Java_Store_Key/nfcoffee_key.jks`

Gradle does **not** hardcode any of this any more. It reads `keystore.properties`
in the project root, which is git-ignored:

```properties
storeFile=/home/cox/AndroidKeysStore/nfcoffee_key.jks
storePassword=…
keyAlias=key0
keyPassword=…
```

If `keystore.properties` is absent, debug builds still work; release builds
stop with an explicit error instead of silently producing an unsigned bundle.

## This is an UPLOAD key, not the app signing key

`ReleaseOutput/Old_Java_Store_Key/` proves the app is enrolled in **Google Play
App Signing** (`pepk.jar`, `encryption_public_key.pem`,
`encrypted_private_key.pem`, `private_key.pepk`, `gen_key_command.sh`).

That means:

- Google holds the **app signing key** and signs what users download.
- `nfcoffee_key.jks` is only used to sign the upload. Google re-signs it.
- **The certificate users see is not `A7:BF:D9:…`.** That is expected.
- If this upload key is ever lost or leaked you request an *upload key reset*
  in Play Console → **Protected with Play → Play Store protection → Manage
  Play app signing**. The app keeps its identity and existing installs keep
  updating. You do **not** need to publish a new app.

## Verifying the keystore

```bash
# JDK bundled with the Android Studio flatpak
KT=/var/lib/flatpak/app/com.google.AndroidStudio/x86_64/stable/active/files/extra/jbr/bin/keytool

$KT -list -v -keystore ~/AndroidKeysStore/nfcoffee_key.jks -storepass 'PASSWORD'
```

Check that the SHA-256 of `key0` still matches the table above.

## Building a signed release

```bash
export JAVA_HOME=/var/lib/flatpak/app/com.google.AndroidStudio/x86_64/stable/active/files/extra/jbr
./gradlew bundleRelease          # -> app/build/outputs/bundle/release/app-release.aab
./gradlew assembleRelease        # -> app/build/outputs/apk/release/app-release.apk
```

Verify what you produced:

```bash
$KT -printcert -jarfile app/build/outputs/bundle/release/app-release.aab
```

## Creating another upload key (reference)

The current new key was generated on 2026-11-14 with exactly this command
(the password is only in the password manager, never in this repo):

```bash
KT=/var/lib/flatpak/app/com.google.AndroidStudio/x86_64/stable/active/files/extra/jbr/bin/keytool

$KT -genkeypair \
  -keystore ~/AndroidKeysStore/nfcoffee-upload.p12 \
  -storetype PKCS12 \
  -alias key0 \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=NFCoffee Upload, O=DSGMedia.ro, C=RO"
```

Notes:

- `-validity 10000` (~27 years). Google requires the key to be valid past
  **22 October 2033**.
- Use **PKCS12**, not JKS. Strongly prefer a 4096-bit RSA key.
- Use the **same** password for store and key: PKCS12 does not support
  different ones.
- Put the password in a password manager before you press Enter.
- `storeFile` may point anywhere; keep it **outside** the repository.

Export the certificate and upload it in Play Console →
**Protected with Play → Play Store protection → Manage Play app signing** →
*Upload key certificate* → **Request upload key reset**
(older documentation still says *Release → Setup → App integrity*; Google
renamed that area). Upload the *certificate*, never the keystore:

```bash
$KT -export -rfc -alias key0 \
  -keystore ~/AndroidKeysStore/nfcoffee-upload.p12 \
  -file ~/AndroidKeysStore/upload_certificate.pem
```

Google reviews the request (turnaround is not published; plan for days).
Until it is approved, keep signing uploads with the **old** key.

## Security notes

- The old keystore password appeared in plain text in `app/build.gradle` on a
  **public** GitHub repository (`RaveGun/NFCaffe`) from 2017 until 2026-11-14.
  The `.jks` itself was never committed. Treat the *password* as compromised:
  rotate it when you create the new key.
- Never commit `keystore.properties`, `*.jks`, `*.p12`, `*.pepk` or `*.pem`.
  `.gitignore` now covers all of them.
- Keep at least two offline backups of the keystore. Losing it is recoverable
  (upload key reset) but annoying; losing it *and* Play Console access is not.
