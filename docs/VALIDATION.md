# v0.3.0 validation

GitHub run https://github.com/rrcee/flowcaredemo/actions/runs/43702615610 builds Patient, Doctor, and Hospital APKs together. Each variant passes Kotlin regression tests and Android lint.

API 35 Pixel 6 emulator test disables Wi-Fi and mobile data, then verifies:

- Demo sign-in in all three independently installed apps.
- Patient booking, review, confirmation, and QR pass.
- The Hospital app receives the shared booking and checks the patient in.
- The Doctor app receives the queue entry, calls the patient, creates a lab bill, shares care instructions, and completes consultation.
- Patient pays the simulated lab charge.
- Hospital marks the order ready.
- Patient retrieves shared care instructions, opens Profile, and keeps navigation state after a 1.3× text-size change.

Native screenshots are uploaded to the run. Representative home, hospital overview, and doctor consultation captures were visually inspected. No physical-device or formal accessibility certification is claimed.

Website: TypeScript and Vite production builds pass, and Sites confirms successful private deployment. Browser interaction QA was unavailable because this managed environment does not expose its required browser-control tool. Reduced-motion and pause/fallback paths were reviewed in source.
