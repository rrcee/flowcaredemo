# Validation

Backend: `pytest -q` — 2 integration tests passed on Python 3.12. Covers booking conflict, cancellation/reindexing, role checks, queue pause, consultation completion, prescriptions, sandbox billing, review eligibility, record consumption/expiry, and WebSocket initial synchronization.

Native: Android and iOS source supplied. Compilation and device validation pending. This Linux environment has no Xcode; initial Gradle/SDK dependency retrieval failed to reach remote repositories without explicit proxy configuration. CI includes Android APK compilation and iOS simulator framework compilation.

No live hospital, payment, OCR, clinician-call, push, or model providers were exercised. Synthetic data only.
