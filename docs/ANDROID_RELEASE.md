Android demo build of FlowCare with patient/family and doctor views.

Includes booking, live queues, emergency pause, lab billing simulation, prescriptions, temporary records, reviews, and scripted grounding support. Design uses bundled Plus Jakarta Sans and JetBrains Mono with the specified muted palette.

Validation: built by GitHub Actions from bbfc0f05524de8827ce6c1d60aa8b0aa85bd8aad; Android compilation, Kotlin regression tests, lint, and four backend integration tests passed. The app launches on an Android emulator. Full journey automation remains unverified; its first run failed in the test harness while collecting the UI hierarchy.

Install `FlowCare-v0.1.0-demo.apk` on Android 8.0 or later. This APK is debug-signed for demo testing; it is not a Play Store production build. SHA256SUMS.txt accompanies the APK.

A running FlowCare backend is required for connected features. Follow README setup and enter the backend computer's LAN URL in app Settings when using a physical phone. Emulator default: http://10.0.2.2:8000.

Synthetic data only. Payments, OCR, clinician calls and mental-health support are demo experiences; production identity and provider integrations are not included.

Source/setup: https://github.com/rrcee/flowcaredemo
Build checks: https://github.com/rrcee/flowcaredemo/actions/runs/43697623763
