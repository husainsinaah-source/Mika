# Mika v0.1.33 — “Hey Mika” voice upgrade

**This is source code and a GitHub APK builder, not an APK.** It extends the previous spoken-replies patch for the **official Mika v0.1.33** source. The Android APK has not been compiled or tested on a device in this environment. The build workflow will attempt to compile and unit-test it on GitHub Actions; it will produce an APK artifact **only if the workflow succeeds**.

## What this adds

- “**Hey Mika, open YouTube**” or “**Mika, set an alarm for 8**”: recognize the wake phrase and send the rest as a Mika task **without typing or pressing send**.
- “**Hey Mika**”: Mika says “Yes?” (if spoken replies are enabled), then you can speak a command in the next ~12 seconds.
- Uses Mika's existing press-and-hold speech-to-text too.
- Mika can speak its completed answers aloud using Android TextToSpeech.
- Turn listening on and off with a microphone icon in the chat header. This starts **OFF**; requires microphone permission. Speaker icon controls spoken replies independently.
- Pauses recognition during push-to-talk, TTS playback, and when the activity is not visible; destroys the recognizer when the chat screen is disposed.

## Important limitations

- **Wake recognition works while Mika's chat is visibly open, not from a locked screen or while using another app.** It does not register a system-wide Android assistant or a low-power, offline hotword engine.
- The wake phrase is **experimental**. It repeatedly uses Android's standard SpeechRecognizer in short sessions. Google explicitly says SpeechRecognizer is **not intended for continuous recognition** because it can use significant battery, network bandwidth, and third-party services. Some Android implementations will stop or throttle repeated recognition; some may require internet. Leave the microphone toggle off when not needed.
- Recognition uses your default system language; this version detects the **English phrase** “Hey Mika” / “Mika”. It may fail if the installed recognition service mishears the name.
- Android speech-recognition providers might send microphone audio to their servers. Check your phone's recognition service privacy policy.
- Spoken replies use Android's installed TTS voices, **not Siri's or Alexa's proprietary voices**.
- Requires Mika's model configured and the existing app permissions granted. It does not remove Mika's normal safeguards around tasks and approvals.

## Generate the APK with GitHub using an Android phone

1. Visit https://github.com/wangzan101/Mika and choose **Fork** to create your own copy.
2. Unzip this builder. Upload these four files to the **root of your fork**: `Mika_voice.patch`, `apply_hey_mika.py`, `WakePhraseListener.kt`, and `MikaWakePhraseTest.kt`. Their names and capitalization must stay the same.
3. Upload the included `.github/workflows/mika-hey-mika-apk.yml` to the **same path** in the fork. If your Android ZIP app hides folders starting with `.`, use GitHub **Add file → Create new file**, enter `.github/workflows/mika-hey-mika-apk.yml`, and paste the file contents.
4. In your fork, open **Actions → Build Mika v0.1.33 with Hey Mika → Run workflow**. If GitHub requires workflows to be enabled, review and enable them for your own fork.
5. If the run succeeds, open it and download **Mika-v0.1.33-Hey-Mika-APK** from the **Artifacts** section. Unzip this artifact to retrieve `app-debug.apk`.
6. **Back up your current Mika chats, settings, keys, or exports if possible before uninstalling anything**. This app has the same package ID (`com.wangzan101.mika`) but a different debug signature; it cannot install over the official Mika APK. Once you have a real working APK and a backup, uninstall the official app if you choose, then install the newly built `app-debug.apk`.
7. Open Mika, configure your model and phone-control settings, grant microphone access, and tap the microphone icon in the chat header. Say: “Hey Mika, what time is it?” Then test “Hey Mika” / “Open YouTube” in separate phrases.

## Builder contents

- `Mika_voice.patch` — adds spoken replies and speaker controls to the original source.
- `apply_hey_mika.py` — adds foreground wake listening and TTS coordination to the patched source.
- `WakePhraseListener.kt` — microphone recognition loop and the wake phrase parser.
- `MikaWakePhraseTest.kt` — JUnit tests for recognized commands and rejected phrases.
- `.github/workflows/mika-hey-mika-apk.yml` — downloads the official tagged v0.1.33 source, applies both changes, tests and builds a **debug-signed universal APK**.

## Build diagnostics

If the workflow shows a red failure, check its `Apply spoken replies and Hey Mika sources` and `Compile and test debug APK` logs. If the repository tag changes or the project dependency build environment changes, the patch may need updating. Do **not** uninstall an installed Mika app based on the existence of this builder ZIP alone.

To get true background / screen-off “Hey Mika” detection, an additional dedicated **on-device wake-word model and user-started microphone foreground service** would be necessary. That is not provided by this experimental version.
