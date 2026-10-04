# Prove2Break — App Restriction

## Manual App Selection

The student picks which installed apps to restrict — the app does not attempt to auto-classify "distracting" apps (that's future ML scope, see `FUTURE-ROADMAP.md`). Typical examples the setup screen surfaces prominently: Instagram, YouTube, TikTok, X/Twitter, games, messaging apps — but the full installed-app list is browsable/searchable so the student can restrict anything.

## Honest Disclosure of Android Limitations

**Prove2Break does not promise perfect, unbypassable app blocking**, because Android does not give a third-party (non-device-owner, non-MDM) app a guaranteed way to prevent another app from launching. The product is explicit about this in-app (e.g. in the App Restriction Setup screen and Settings), rather than overselling a "100% blocked" guarantee it cannot keep. Framing: "Prove2Break makes it meaningfully harder to open distracting apps during focus — not impossible. It's a strong nudge and friction layer, not a lock."

Known limitations to disclose:
- A determined user can always find a way around app-level restriction on a general-purpose Android device without MDM/device-owner privileges (e.g. via Safe Mode, uninstall/reinstall, a secondary profile, or simply granting themselves an exception).
- Detection of "which app is in the foreground" relies on `UsageStatsManager`, which has inherent latency (checks on an interval, not instant), so there can be a brief window where a blocked app is visible before the interstitial appears.
- OEM-specific battery optimization can suspend background detection unless the user is guided to exempt the app (the permission explainer screen should mention this where relevant).

## Possible Mechanisms

- **`UsageStatsManager`** — poll (or use `UsageEvents`) to detect the current foreground app and trigger the blocked-app interstitial when a restricted package is detected. Requires the "Usage Access" special permission, granted via system settings (not a runtime permission dialog).
- **Accessibility Service** — where a more immediate/reliable foreground-app detection is needed, an Accessibility Service can observe window state changes. Used only if `UsageStatsManager` polling latency proves insufficient in testing; accessibility services carry a higher user-trust bar and Play Store review scrutiny, so this is adopted deliberately, not by default.
- **Foreground service** — keeps restriction/detection logic alive alongside the focus timer service (may be the same service or a closely coordinated one).
- **Notifications** — used to inform, not to enforce; e.g. if a restricted app is detected in the background attempting to post a notification, that's out of scope for blocking in MVP (only foreground launches are intercepted).
- **Supported device-management APIs** — not used for MVP (that implies MDM/enterprise enrollment, out of scope for a consumer study app), noted here only to record that it was considered and deliberately excluded.

## Requirement: Explain Every Sensitive Permission

Before requesting Usage Access (or, if adopted, Accessibility Service), the app shows a plain-language explainer screen: what the permission is, exactly why Prove2Break needs it (to detect when a restricted app is opened during focus), what happens if the student declines (restriction simply won't work — the focus timer and everything else still functions), and a direct link to the system settings screen where it's granted (these permissions cannot be requested via a standard runtime dialog). See `PERMISSIONS.md` for the full permission-by-permission table.
