# Bug fixes and checks

- Fixed Android CI requesting the obsolete SDK `tools` package; specified API 35 packages and added downloadable build artifacts.
- Added Kotlin regression tests for FastAPI validation errors and role-aware Back navigation.
- Added Android system Back, edge-to-edge initialization and keyboard resize/inset support.
- Fixed stale selected-booking filtering in All bills.
- Cancelled connection-scoped requests when role/server changes; reset old state and sensitive UI fields. Preserved coroutine cancellation and added request timeouts.
- Distinguished a saved action from a subsequent failed refresh to discourage duplicate submissions.
- Added readable structured validation errors, whitespace rejection and normalized visit-time validation.
- Invalid doctor queue actions return 404; orders/prescriptions require an active consultation.
- Cancellation removes unpaid bills and temporary records; inactive visits cannot receive temporary uploads.
- Malformed WebSocket authentication closes cleanly.
- Added empty/reconnect states, wrapping controls, bundled fonts, themed foregrounds and paused queue-estimate copy.

Backend: four integration tests pass locally. Native build status and artifacts are recorded by GitHub Actions. No claim of live payment/OCR/call integration or production authorization is made.
