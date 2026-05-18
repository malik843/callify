# Callify — Physical Device Test Checklist
## Pre-Test Setup
- [ ] Install DEBUG build on physical Android device (not emulator)
- [ ] Grant READ_PHONE_STATE at prompt
- [ ] Grant SYSTEM_ALERT_WINDOW in system settings
- [ ] Grant POST_NOTIFICATIONS at prompt (API 33+ only)
- [ ] Confirm "Callify is listening for incoming calls"
      notification is visible in notification shade
- [ ] Confirm API base URL is set correctly in BuildConfig

## Core Loop Tests
- [ ] TC-01: Incoming call from a number IN the database
        Expected: Overlay shows correct name and address
        within ~1 second of first ring
- [ ] TC-02: Incoming call from a number NOT in the database
        Expected: Overlay shows "Unknown Caller"
- [ ] TC-03: Decline call immediately after ring
        Expected: Overlay disappears, no crash
- [ ] TC-04: Answer call — overlay behaviour
        Expected: Overlay disappears on OFFHOOK or IDLE
- [ ] TC-05: Call same number twice in a row
        Expected: Second lookup hits LRU cache, overlay
        appears faster than TC-01
- [ ] TC-06: Incoming call with private/withheld number
        Expected: Overlay shows "Unknown Caller",
        no API request fired (check Logcat)

## Edge Case Tests
- [ ] TC-07: Airplane mode ON — incoming call (if possible on device)
        Expected: Overlay shows "No connection"
- [ ] TC-08: Server unreachable (wrong API URL in BuildConfig)
        Expected: Overlay shows timeout or network error state
- [ ] TC-09: Rapid back-to-back calls (call waiting simulation)
        Expected: Previous overlay dismissed before new one appears
- [ ] TC-10: Revoke SYSTEM_ALERT_WINDOW mid-session via settings,
        then receive a call
        Expected: No crash, Logcat shows permission warning
- [ ] TC-11: Force-stop and relaunch app
        Expected: Service restarts, notification reappears
- [ ] TC-12: Device rebooted — open app and confirm service
        starts correctly after permissions re-confirmed

## Release Build Tests
- [ ] TC-13: Build RELEASE APK — confirm it compiles without error
- [ ] TC-14: Install release APK — confirm all features work
        identically to debug build
- [ ] TC-15: Open Logcat with release build — confirm NO Log.d
        output appears from "Callify" tag
- [ ] TC-16: Confirm overlay renders correctly on:
        - [ ] Small screen (< 5.5 inch)
        - [ ] Large screen (> 6.5 inch)
        - [ ] Device with notch / punch-hole camera

## Sign-Off
- Tested by: _______________
- Device model: _______________
- Android version: _______________
- Date: _______________
- Release build SHA: _______________
