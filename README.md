# FlowCare — St. Mathew Hospital offline demos

Three distinct native Android apps: Patient, Doctor, and Hospital. Each has its own icon, demo sign-in, bottom navigation, menu, profile and focused screens. St. Mathew Hospital and all sample identities are fictional.

## Install

Install all three APKs from the same release on a single Android 8+ device, Hospital first. Tap **Use demo account** in each app. The apps work without internet or a backend.

When the Hospital app is installed, a signature-protected Android ContentProvider shares on-device demo visits, bills and care instructions. Apps installed on different devices do not synchronize. Without Hospital installed, each app uses its own local sample store. Local demo data remains on the device between launches; reset it in Settings. Uninstalling the Hospital app removes its shared state.

## Complete demo

1. Patient: Home → Book an appointment → patient → doctor → date/time → review → confirm. Open the QR pass.
2. Hospital: Check-in → select the booking → Check in.
3. Doctor: Queue → select patient → Call patient → order a blood test → share fictional care instructions → Complete consultation.
4. Patient: Bills → simulate payment; Visits → completed visit → Care instructions.
5. Hospital: Orders → mark the paid lab request ready.

Payments, sign-in and care workflows are simulations. Do not enter real credentials, medical records or personal data. This is not a clinical product, emergency service or live hospital system.

## Build

JDK 17, Gradle 8.11.1, Android SDK 35:

```sh
gradle :composeApp:assembleDebug
```

APKs are under `composeApp/build/outputs/apk/{patient,doctor,hospital}/debug/`. All three must be signed with the same certificate for shared data access. CI builds them together and runs the complete journey on an Android emulator with Wi-Fi and mobile data disabled.

The former FastAPI/iOS prototype remains in the repository as historical source. The three current Android demo apps do not call it and require no backend.
