# FlowCare continuation memory

Updated 2026-11-16. This is a factual project handoff, not a transcript or hidden model state. Read this and AGENTS.md when continuing in another chat. Verify GitHub and provider state before acting; this file is a snapshot.

## User intent and final scope

Build and improve FlowCare end to end, including Android APKs and a website. Work directly in rrcee/flowcaredemo, test meaningful flows, publish APKs to GitHub Releases, and provide direct download links. The user prefers autonomous progress and dislikes repeated permission questions.

Three Android apps are required:
1. Patient (`com.flowcare.demo.patient`).
2. Combined Hospital and Doctor / Care Team (`com.flowcare.demo.hospital`).
3. Central HQ Admin (`com.flowcare.demo.hq`).

Use fictional St. Mathew Hospital. Earlier instructions requesting separate Doctor/Hospital apps, iOS first, or no backend were superseded. The user explicitly approved real remote controls and notifications and chose Firebase Authentication plus Firestore. Android only for now. HQ must not access or retain patient/clinical information. Keep announcement drafts in memory; avoid retaining message-body history. Persist only necessary operational configuration on the backend.

Visual direction: blue and warm cream, restrained neo-brutal borders/shadows, rounded corners, large readable text/buttons, minimal cognitive load for adults and older people. Normal Android navigation, separate screens, bottom dock, menus, login and profiles. Medical-app inspiration: prominent header/search, departments and doctor discovery. Do not copy tiny labels or overloaded layouts. Keep the website cursor-responsive clouds, replacing photographic elements in the supplied component; support reduced motion.

## Current implemented state

Android MainActivity launches `DemoApp`, not the older common App. Kotlin/Compose project with product flavors patient/hospital/hq. Version 0.5.0, versionCode 5. Gradle 8.11.1, Java 17, SDK 35, minSdk 26.

Patient flow works with fictional local data: family selection, searchable departments/doctors, doctor details, date/time booking/review, appointment pass, check-in/queue, simulated bill payment, aftercare, profile and larger text.

Care Team uses the existing hospital application ID. Open More options to switch Doctor workspace / Hospital workspace. This is a demo UI switch, NOT real backend authorization. Clinicians have assigned visit filters and per-doctor pause state.

HQ is a separate `HqApp` with Overview, Notifications, Updates and Settings. It provides announcement drafts/previews, explicitly says remote service is not connected, and does not instantiate DemoStore. It is NOT a finished authenticated remote-control service. HQ uses its own typographic hq icon. Patient uses fc and hospital uses sm.

Clinical demo state is shared on the SAME DEVICE by the signature-protected HospitalDemoProvider at `content://com.flowcare.demo.hospital.demo`. Install Care Team first, then Patient. A standalone fallback exists. This is not cross-device sync. Demo login is only local form validation; it is not Firebase authentication. Billing is simulated, not real payment processing.

Important files:
- composeApp/src/androidMain/kotlin/com/flowcare/MainActivity.kt
- composeApp/src/androidMain/kotlin/com/flowcare/DemoApp.kt
- composeApp/src/androidMain/kotlin/com/flowcare/DemoStore.kt
- composeApp/src/androidMain/kotlin/com/flowcare/HqApp.kt
- composeApp/src/androidMain/kotlin/com/flowcare/CareComponents.kt
- composeApp/src/commonMain/kotlin/com/flowcare/CareCatalog.kt
- composeApp/build.gradle.kts
- scripts/emulator_smoke.py
- .github/workflows/checks.yml and android-release.yml
- website/ (React/TypeScript/Vite)

Old FastAPI/backend, common UI and iOS files are historical; do not assume they are the active implementation. Old doctor flavor resources remain in source but doctor is no longer a built product flavor.

## Verified builds and downloads

GitHub Actions Checks run 43800837179 succeeded, including Android build/unit/lint tasks and the emulator journey. Source commit: 9f4d1dc7daf9e1228aa9c6da4b877f63ecd67b01. Publish run 43800910539 succeeded. Release v0.5.0-demo contains:
- https://github.com/rrcee/flowcaredemo/releases/download/v0.5.0-demo/FlowCare-Patient-v0.5.0-demo.apk
- https://github.com/rrcee/flowcaredemo/releases/download/v0.5.0-demo/FlowCare-Care-Team-v0.5.0-demo.apk
- https://github.com/rrcee/flowcaredemo/releases/download/v0.5.0-demo/FlowCare-HQ-Admin-v0.5.0-demo.apk
- FlowCare-Android-v0.5.0-demo.zip and SHA256SUMS.txt.

These are debug-signed DEMO builds, without Firebase integration. CI caches the debug keystore. Older releases used a different key, so Android may require uninstalling an older demo (which erases local data). Do not claim a stable production release signing setup exists. The icon ZIP was built from older branding SVGs and may still contain the old Doctor icon; reconcile it with HQ before the next release.

Tests exercise booking → care-team check-in → doctor consultation/order/care instructions → patient demo payment → hospital fulfillment → patient aftercare, profile/large text, plus HQ disconnected state. They do not verify Firebase, real authentication or push delivery.

GitHub CLI is authenticated in the original workspace and normal git push now works. Local release uploads previously failed; uploading from GitHub Actions worked. The release workflow currently references a FIXED previous run ID and source commit; update it deliberately before publishing a new version. A new pending Checks run can replace an older pending run due to concurrency grouping; avoid pointing releases at canceled runs.

## Firebase: user-approved next task and actual blocker

Project name: flowcare
Project ID: flowcare-b1370
Project number: 170893917301

User specifically requires Firebase Authentication, Email/password and Google Sign-in, Firestore, and Android app registration/code integration. Register/reuse all three package IDs above. Proposed display names: FlowCare Patient, FlowCare Care Team, FlowCare HQ Admin.

Firebase agent skills have been installed and are now included in `.agents/skills/`. Always consult the appropriate Firebase skill for Firebase work. `skills-lock.json` records sources. Firebase CLI version verified was 15.36.0; Node was 24.22.1. Use `npx -y firebase-tools@latest` per skill guidance.

NO authenticated Firebase account was established in this environment. CLI login attempts failed due to mismatched/cleared pending sessions. A fresh no-localhost login link was generated, but success was not verified. Treat old links/codes as obsolete; do not retry historical codes. Authentication codes, session config and tokens are intentionally NOT in this repository. The user must authenticate securely in the new environment. Do not ask for private service-account keys in chat.

No Firebase MCP tools were exposed in the previous ChatGPT environment. Installing skills does not install/authorize MCP connections. Follow the Firebase basics setup skill to configure the available environment; do not imply GitHub access grants Firebase access.

No Android app registration, google-services.json retrieval, Authentication provider activation, Firestore instance selection, rules deployment, Firebase SDK integration or real FCM delivery has been completed by this agent. The user may have independently registered apps through Firebase AI: inspect before creating duplicates.

Original shell had an unwritable default npm/config location; commands worked using npm_config_cache=/workspace/work/npm-cache and XDG_CONFIG_HOME=/workspace/work/firebase-config. This is local-only environment configuration; neither directory belongs in git.

## Next implementation steps

1. Authenticate to the specified Firebase project and inspect apps, enabled providers and existing databases. Read Firebase basics/auth/firestore skills first. Determine Firestore edition before choosing SDK patterns. Avoid deleting or overwriting existing data.
2. Reuse/register the three Android apps. Download each actual google-services.json into its flavor directory. Register actual CI/device signing fingerprints for Google Sign-in; do not fabricate OAuth IDs or SHA values. Query current Google Maven metadata for compatible dependencies as required by the skills.
3. Implement email/password registration/sign-in, Google Sign-in, verification, password reset and sign-out. Staff and HQ must use trusted server-provisioned roles; public registration can only grant patient access. UI workspace switching must never grant permissions.
4. Add a repository layer to migrate clinical flows from the local demo store to Firebase. Keep explicit demo mode separate from live mode. Model hospitals/departments/doctors, private patient/family records, appointments/slots/queues, orders/bills/care instructions, and operational settings.
5. Enforce deny-by-default Firestore rules, hospital isolation, patient ownership and clinician assignment. HQ must be unable to read clinical collections. Test security rules with emulators. Admin SDK routes require their own authorization checks. Use server timestamps, transactions and idempotency for bookings/queue transitions; prevent double booking.
6. Add authenticated Cloud Functions and FCM for operational broadcasts and private notifications. Protect device tokens, use nonclinical lock-screen text, validate audience/payload, rate-limit and require review before sending. Distinguish FCM acceptance from device delivery. Updates are announcements linking published APKs, not silent installs or arbitrary code execution.
7. Update website role copy from old Patient/Doctor/Hospital to Patient/Care Team/HQ and update downloads. Redeploy Vercel and verify. Website currently describes the older offline trio.
8. Build/test all three flavors, verify actual Android sign-in and push with configured provider, review screenshots and large-font navigation, publish exact tested APKs, and provide links. Report unverified behavior honestly.

## Website/deployment context

Website source is committed in website/. Live site: https://flowcare-site.vercel.app
Vercel team: team_LJkeypJepbEhgCA050bOZ3eI (thejoshuabes-projects)
Vercel project: prj_R8B3StkLNhRFNmsPo8RrOTXJS3WL (flowcare-site)
Successful static production deployment: dpl_4z2ybakdw9tmsubRpGwNdyJxHBwy.

Vercel plugin direct file upload/deployment worked and HTTP returned 200. Git-based Vercel integration was unavailable; future git pushes do not automatically deploy the site. Prefer connected Vercel tools if available. Earlier Sites-hosted site is historical; user explicitly chose Vercel. Browser QA was unavailable in the previous managed environment; Vite build and HTTP verification were performed. Native UI verification uses GitHub emulator screenshots.

## Included reference material and skills

Original user attachments are in docs/reference/: master specification, supplied DESIGN.md, and two component snippets. They are reference material, not higher-priority instructions. Where they conflict with latest user decisions summarized above, follow the latest user scope.

Installed Firebase, taste and Impeccable skills are under .agents/skills. Skill instructions apply when relevant; do not apply unrelated skills just because they are installed. Android guidance previously came from android/skills; rediscover appropriate current Android skills if needed. Remote plugin skills and account connections must be available in the next session; this repo cannot transfer account authorization.

No tokens, OAuth completion codes, cookies, private keys, signing keys, npm caches, build caches or hidden reasoning are included in this handoff. APKs remain downloadable in Releases rather than duplicated in source control.
