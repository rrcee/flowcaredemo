# FlowCare v0.3.0 — three offline Android demos

Patient, Doctor and Hospital apps for fictional **St. Mathew Hospital**. Each app has its own typographic icon, demo sign-in, persistent bottom navigation, profile, menu, and focused screens. The Patient app includes family profiles, booking, QR passes, live queue state, bills and aftercare. Doctor manages the queue, consultations, orders and care instructions. Hospital handles check-in and department fulfillment.

## Install and try

1. Install **FlowCare-Hospital-v0.3.0-demo.apk first**, followed by Doctor and Patient, on the **same Android 8+ device**.
2. Tap **Use demo account** in each app. No account registration, internet or server is needed.
3. Patient: book a visit, then open its QR pass.
4. Hospital: Check-in → select the booking → Check in.
5. Doctor: Queue → select patient → Call patient → order a blood test → share fictional care instructions → Complete consultation.
6. Patient: Bills → simulate payment; Visits → completed visit → care instructions.
7. Hospital: Orders → mark the lab request ready.

Shared data stays on the device through the Hospital app's local provider. Apps on different devices do not synchronize. Installing only one app uses an independent local demo store. Reset sample data in Settings.

**Verification:** all three APKs compile; unit tests and Android lint pass. The full cross-app journey passes on an API 35 emulator with Wi-Fi and mobile data disabled, including profile navigation and large-text checks. Build and native screenshots: https://github.com/rrcee/flowcaredemo/actions/runs/43702615610

APK source: 7a65825b50f2acdb8ab4028dd6b51ea8f7533c2f.

These are debug-signed offline demos. Sign-in, payments and clinical workflows are simulations. Do not enter real credentials or medical data. No live hospital services, clinical advice, OCR provider, AI provider, emergency dispatch or production identity system is included.

Companion website: https://flowcare-room-to-breathe.pradeep-mathan.chatgpt.site
