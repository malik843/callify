# Callify — Agent Context

## What is Callify?

Callify is a native Android application (Kotlin, min SDK 26) that acts as a real-time caller identification layer on top of the Android dialer. It remains completely dormant when no call is active, consuming negligible resources, and wakes only at the exact moment an incoming call is detected. Its sole purpose is to identify who is calling — before the user decides to answer — by querying a remote database and displaying the result as a floating overlay on the screen.

Callify is not a dialer replacement, not a call recorder, not a spam filter, and not a contacts manager. It is a focused, single-responsibility identification tool.

---

## The Core Story (What Happens, Step by Step)

### 1. Dormancy
The app runs a lightweight foreground service (`CallDetectorService`) that registers a `PhoneStateListener` with the system's `TelephonyManager`. Outside of an active call, this service does nothing except listen. No network requests are made. No UI is shown. The app is invisible.

### 2. The Trigger — RINGING
The moment `CALL_STATE_RINGING` is detected, the service extracts the incoming phone number from the system and immediately fires an HTTP POST request to the Callify backend API (a custom HTTPS endpoint already in production). The request body contains only the phone number.

### 3. The Race Against Time
Latency is the product's critical constraint. The user is looking at their screen right now deciding whether to answer. The entire round-trip — device fires request → server queries database → server responds → app renders overlay — must complete in as few milliseconds as possible. The backend queries a dataset containing: `firstname`, `lastname`, `phone`, `address`.

### 4. The Overlay
While waiting for the API response, Callify immediately renders a skeleton/loading overlay using Android's `SYSTEM_ALERT_WINDOW` permission via `WindowManager`. When the API responds, the overlay populates with the caller's name and address. If no record is found, it displays "Unknown Caller." If the API fails or times out, it displays "Lookup failed." The overlay sits on top of the native Android dialer — the user can still see and interact with the dialer behind it.

### 5. Teardown — IDLE
When `CALL_STATE_IDLE` is detected (call ended, declined, or missed), the overlay is immediately dismissed and all resources tied to that call lifecycle are released. The service returns to its dormant listening state. No state is persisted between calls except an in-memory LRU cache for recently looked-up numbers.

---

## Architecture Overview

```
Android System (TelephonyManager)
        │
        │ CALL_STATE_RINGING
        ▼
CallDetectorService          ← Foreground Service (always alive)
        │
        │ phone number
        ▼
CallerRepository
        │
        ├──► CallerApiClient (Retrofit + OkHttp)
        │         │
        │         │ POST /lookup { phone }
        │         ▼
        │    [Your HTTPS API]
        │         │
        │         ▼
        │    Response: { firstname, lastname, address }
        │              or 404 / error
        │
        ▼
OverlayManager
        │
        ▼
CallerOverlayView (floating XML layout)
        │
        │ CALL_STATE_IDLE
        ▼
Teardown → overlay dismissed → service sleeps
```

---

## Key Files and Their Roles

| File | Purpose |
|---|---|
| `CallDetectorService.kt` | Foreground service; registers phone state listener; orchestrates the call lifecycle |
| `CallerRepository.kt` | Single source of truth; coordinates API call, caching, and result delivery |
| `CallerApiClient.kt` | Retrofit interface + OkHttp client; all network configuration lives here |
| `CallerInfo.kt` | Data class: `firstname`, `lastname`, `phone`, `address`, nullable fields |
| `OverlayManager.kt` | Handles `WindowManager.addView()` and `removeView()`; owns the overlay lifecycle |
| `overlay_caller.xml` | The floating UI layout; name, address, loading skeleton, dismiss button |
| `MainActivity.kt` | Permission setup only (READ_PHONE_STATE, SYSTEM_ALERT_WINDOW); otherwise dormant |
| `PermissionHelper.kt` | Utility for checking and requesting required permissions at runtime |

---

## Permissions Required

| Permission | Why |
|---|---|
| `READ_PHONE_STATE` | Detect incoming call state and extract the phone number |
| `SYSTEM_ALERT_WINDOW` | Draw the overlay on top of the native dialer |
| `INTERNET` | Make HTTP requests to the Callify API |
| `FOREGROUND_SERVICE` | Keep `CallDetectorService` alive during a call |
| `FOREGROUND_SERVICE_PHONE_CALL` | Required on Android 14+ for phone-call foreground services |

---

## API Contract

- **Method:** POST  
- **Endpoint:** `[CALLIFY_API_BASE_URL]/lookup` *(base URL injected at build time via `BuildConfig`)*  
- **Request body:**
  ```json
  { "phone": "+2348012345678" }
  ```
- **Success response (200):**
  ```json
  {
    "firstname": "Ada",
    "lastname": "Okafor",
    "address": "12 Broad St, Lagos"
  }
  ```
- **Not found:** `404` → display "Unknown Caller"  
- **Error / timeout:** any other status or network failure → display "Lookup failed"

---

## Latency Strategy

- OkHttp connection pool pre-warmed when the service starts
- In-memory LRU cache (last 50 numbers) — repeated callers resolve in 0ms
- Aggressive timeouts: connect 3s, read 4s — after which the overlay shows a failure state, never hangs
- Skeleton loading UI shown immediately on RINGING so the user always sees *something* within milliseconds

---

## What Callify Is NOT

- Not a replacement for the native Android dialer
- Not a spam detection or robocall blocking service
- Not a contacts sync tool
- Not a call recorder
- Not a VoIP or calling application
- Not a background data harvesting tool — it only queries on an active RINGING event

---

## Future Features (Do Not Implement Yet)

These are planned but explicitly out of scope for current development:

- Post-call summary screen
- Call history log
- User-submitted caller reports / crowd-sourced data
- Spam scoring or call blocking
- UI theming / dark mode customisation
- Notification-based ID (as an alternative to the overlay)

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| Min SDK | 26 (Android 8.0) |
| Networking | Retrofit 2 + OkHttp 4 |
| Async | Kotlin Coroutines + StateFlow |
| DI | *(to be decided — Hilt recommended)* |
| UI | XML layouts (no Compose for overlay — WindowManager requires View) |
| Build | Gradle (Kotlin DSL) |
