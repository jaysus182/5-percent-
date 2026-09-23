# 5%

Save 5%. Stay Connected.

5% is a lightweight Android prototype that reads the phone's real battery state and helps keep the final reserve available for essential communication.

## Build and install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The GitHub Actions workflow builds the same debug APK and publishes it as the `5-percent-debug-apk` artifact.

The app uses Android's normal dialler, contacts, location permission, and sharing sheet. It does not contact emergency services or attempt to power off the phone automatically.
