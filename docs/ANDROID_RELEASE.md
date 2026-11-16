# FlowCare 0.5.0 demo

Three installable Android apps for Android 8 or newer:
- Patient: appointments, check-in, queue, bills and aftercare.
- Care Team: St. Mathew Hospital front desk and doctor workspaces. Use the top-right More options menu to switch workspaces.
- HQ Admin: operational overview, announcement drafts/previews and connection status. No patient-data access in its UI or code path.

Install Care Team first, then Patient on the same device to share fictional clinical demo state. Use “Use demo account”. These are debug-signed demonstration builds, not production medical software. If Android reports a signing conflict with an older demo, uninstall that old demo before installing; uninstalling erases its local demo data.

HQ remote sending, authentication and maintenance controls are not configured. Announcement previews do not send notifications. Firebase and a secure backend still require provisioning. The clinical workflow works offline; this release does not synchronize clinical records across devices.
