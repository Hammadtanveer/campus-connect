# feat: Replace Subscription with Downlode (Downloads) UI + icons, partial M3 nav, and OSS scaffolding

## Summary
- Replace drawer item "Subscription" with "Downlode" (Downloads) and add a UI-only screen (no backend/storage yet).
- Change bottom bar label from “Home” to “Notes” (same route and icon logic).
- Use book icon for Downlode in the drawer and within the screen.
- Partial Material 3 migration in bottom navigation (NavigationBar/NavigationBarItem), keeping other M2 UI stable for now.
- Project rename and cleanup already applied earlier (CampusConnect, `com.example.campusconnect`).
- Open-source scaffolding added (MIT license, README, CONTRIBUTING, Code of Conduct, Security, GitHub issue/PR templates, basic CI).

## Changes
- Screen / navigation
  - `Screen.kt`: add `DrawerScreen.Downlode` (route `downlode`) and remove `Subscription`.
  - `MainView.kt`: wire `DrawerScreen.Downlode` → `DownlodeView()`; fix Seniors/Societies mapping; switch bottom nav to M3 `NavigationBar`.
- Downlode (Downloads) screen
  - `ui/theme/DownlodeView.kt` & `ui/theme/SubscriptionView.kt`: UI-only empty state, Book icon, Material 3 components.
- Icons
  - Drawer Downlode → `outline_book_2_24`.
  - DownlodeView → uses the same book icon.
- Theming / naming (from prior commits in this branch)
  - App name "CampusConnect", `applicationId` & `namespace` `com.example.campusconnect`.
  - Theme `Theme.CampusConnect`, Compose theme `CampusConnectTheme`.
- OSS scaffolding
  - `LICENSE` (MIT), `README.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`.
  - `.github/ISSUE_TEMPLATE/*`, `.github/PULL_REQUEST_TEMPLATE.md`, `.github/workflows/android-ci.yml`.
- Local-only
  - PRD under `local_only/` and ignored in `.gitignore`.

## Notes
- This PR focuses on UI and routing; no backend for downloads yet.
- Remaining M2→M3 migration can be handled separately (Scaffold, ModalBottomSheetLayout, MaterialTheme).

## Testing
- Build: `./gradlew assembleDebug` (or `gradlew.bat assembleDebug` on Windows).
- Manual smoke: launch app, open drawer → Downlode shows placeholder UI with book icon; bottom bar shows "Notes" tab.

## Screenshots
_(Optional)_

## Next steps
- Implement real downloads persistence (Room cache + WorkManager & Storage).
- Complete M3 migration to remove remaining warnings.

