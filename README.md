# FlowCare native demo

Kotlin Multiplatform + Compose UI for Android/iOS, with a FastAPI REST/WebSocket backend. Two demo roles run in one application. Uses only synthetic data; this is an initial functional demo, not a hospital-ready clinical product.

## Start the backend

```sh
cd backend
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```

API explorer: http://localhost:8000/docs. Demo credentials: `X-Token: patient-demo` or `doctor-demo`. Bind to localhost unless testing on a trusted local network. All demo patients share one tenant; do not enter real medical or personal data.

## Android

Install JDK 17+, Android SDK 35 and Gradle 8.11.1, or import this repository into Android Studio and use that Gradle version. Set `sdk.dir` in local.properties when necessary.

```sh
gradle :composeApp:assembleDebug
```

Install composeApp/build/outputs/apk/debug/composeApp-debug.apk. Default server is http://10.0.2.2:8000 for the Android emulator. Change it in Settings for physical devices. Cleartext HTTP is enabled solely for local demo development.

## iOS

Requires macOS, Xcode, JDK 17+, Gradle 8.11.1 and XcodeGen (`brew install xcodegen`).

```sh
gradle :composeApp:linkDebugFrameworkIosSimulatorArm64
cd iosApp
xcodegen generate
open FlowCare.xcodeproj
```

Choose an Apple Silicon iOS simulator and run. The generated project references the simulator framework. For a physical device, build linkDebugFrameworkIosArm64, update FRAMEWORK_SEARCH_PATHS accordingly, and configure your signing team. Default backend: http://localhost:8000. iOS app project/device signing must be validated on macOS.

## Try the connected flow

1. Start backend and open two app instances.
2. Patient: choose a family profile, hospital, doctor, and unique visit time; confirm booking.
3. See a stable appointment reference and changing live token/queue position.
4. Doctor: choose that doctor, call next, open consultation, order a lab and issue a prescription.
5. Patient: see bills, simulate payment, paste a synthetic temporary record.
6. Doctor: consume history once; pause/resume or complete consultation.
7. Patient: view prescription and rate the completed consultation.
8. Try cancellation from a waiting appointment, reconnect after network loss, dark mode and scripted EduMind support.

## Implemented

Persistent SQLite appointments, slot conflict handling, live WebSocket snapshots/reconnect, queue reindexing with stable booking references, pause/resume, doctor consultation, prescriptions, bill settlement simulation, completed-visit reviews and weighted doctor rankings. Encrypted in-process records expire after 30 minutes or first doctor consumption. UI has family selection, discovery, registration, queue, bills, records, consultation, prescription, support and connection settings.

## Integration boundaries

Payments, hospital directory/location, walk-in registration, and mental-health support are demo experiences. QR camera scanning, OCR, external hospital records, live clinician calls, payment providers, PDF export, push notifications, crisis dispatch, production authentication and family consent enforcement are not implemented. No machine learning training or external language model is used. Prescriptions and billing are synthetic workflows.

Temporary record encryption does not establish forensic memory erasure. Clients and runtime memory can retain copies. Production needs a reviewed retention design, authenticated per-patient access, secret handling, TLS, tenant isolation, provider integrations, audit controls that exclude sensitive temporary content, and clinical workflow review. Demo pause state and temporary records reset on restart; SQLite operational records persist. Backend must run as one worker for the in-process live state.

## Tests

```sh
cd backend
pytest -q
```

CI checks backend behavior, Android compilation/lint/Kotlin regression tests, iOS simulator application compilation, and an Android emulator booking smoke test. Download APKs, simulator builds, and native screenshots from the Actions run artifacts. Local verification status is recorded in docs/VALIDATION.md.
