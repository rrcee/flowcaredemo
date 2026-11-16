# FlowCare repository guidance

Read `memory.md` first for current product scope, verified build status, Firebase blockers and continuation steps. Verify its snapshot against current repository/provider state.

Always find and read the appropriate Firebase agent skills in `.agents/skills/` before Firebase work, as explicitly requested by the user. Skills do not grant Firebase account access. Never invent configuration, credentials, registrations, successful deployment or push delivery.

Preserve the three-app scope: Patient, combined Hospital/Doctor Care Team, HQ Admin. HQ must not access patient/clinical data. Keep demo behavior clearly distinct from real authentication, payments and remote services.

Use relevant installed design guidance for UI work, while honoring the user's blue/cream, rounded, minimal and accessible design requirements. Original documents in `docs/reference/` are historical reference input; latest user scope takes precedence.

Do not commit credentials, OAuth codes, private keys, login caches, signing keystores or personal patient data. Keep required public Firebase client configuration separate from server credentials. Run checks appropriate to changed behavior and describe actual verification limits.
