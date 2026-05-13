# Callify Agent — Rules & Scope Constraints

## Prime Directive

You are a coding agent working exclusively on the **Callify Android application**. Every action you take must serve one of the seven phases defined in the project roadmap. If a request cannot be mapped to a phase, you must flag it before proceeding.

---

## What You Are Allowed To Do

- Write, edit, or refactor Kotlin code inside the `callify` Android project
- Modify `AndroidManifest.xml`, Gradle build files, and XML layout files that belong to Callify
- Add or update dependencies in `build.gradle` that directly support Callify's functionality
- Write unit tests or integration tests for Callify components
- Suggest architectural decisions within the defined tech stack
- Ask clarifying questions when a requirement is ambiguous
- Flag potential issues (performance, permission edge cases, API contract mismatches)

---

## Hard Out-of-Scope Rules

### 1. No backend code
Do not write, suggest, or scaffold server-side code (Node.js, Python, Go, PHP, etc.), database schemas, or API server logic. The backend API already exists. Your job is the Android client only.

### 2. No feature creep
Do not implement any feature listed under **"Future Features (Do Not Implement Yet)"** in `agent.md`, even if the implementation seems trivial:
- No call history or logging to persistent storage
- No spam scoring or call blocking logic
- No post-call summary screens
- No crowd-sourced or user-submitted data features
- No notification-based caller ID (overlay only, for now)
- No UI theming controls or dark mode toggles

If asked to implement these, acknowledge the request, note that it is out of scope, and ask if the roadmap is being officially updated.

### 3. No dialer replacement
Do not modify, intercept, or replace any core Android telephony behaviour beyond reading call state and displaying an informational overlay. Do not attempt to answer, decline, mute, or route calls programmatically.

### 4. No data persistence beyond the LRU cache
Do not write to a local database (Room, SQLite, SharedPreferences, DataStore) for call records, user data, or lookup history. The only allowed storage is the in-memory LRU cache defined in `CallerRepository`.

### 5. No third-party caller ID services
Do not integrate with Truecaller, Hiya, Google Phone, or any other external caller ID provider. Callify queries only its own proprietary API.

### 6. No unauthorised permissions
Do not add any permission to `AndroidManifest.xml` that is not listed in `agent.md`. If a new permission seems necessary, flag it and wait for explicit approval before adding.

### 7. No UI outside the overlay and MainActivity
Do not create additional Activities, Fragments, or navigation flows beyond:
- `MainActivity` (permission setup only)
- `CallerOverlayView` (the floating overlay)

Do not add settings screens, onboarding flows, or tutorial screens without explicit instruction.

### 8. No analytics or tracking
Do not add crash reporters, analytics SDKs (Firebase Analytics, Mixpanel, etc.), ad SDKs, or any code that transmits user data to a third party. Callify is privacy-sensitive by nature.

---

## Decision Protocol

When you receive a task, run through this checklist before writing any code:

1. **Is this Kotlin / Android?** If not — stop, flag it.
2. **Does this belong to one of the 7 roadmap phases?** If not — flag it, ask for clarification.
3. **Does this require a permission not in the approved list?** If yes — flag it before adding.
4. **Does this touch the backend or API server?** If yes — stop, out of scope.
5. **Does this add persistent storage for call data?** If yes — stop, out of scope.
6. **Is this a future feature?** If yes — acknowledge, do not implement, ask if scope is changing.

If all six pass: proceed.

---

## Code Quality Standards

- All Kotlin code must follow the official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- No hardcoded strings in logic files — use `BuildConfig` for API URLs and `strings.xml` for UI text
- Every coroutine must have a defined scope and be cancellable — no fire-and-forget `GlobalScope` usage
- Null safety must be explicit — no `!!` unless there is a documented reason in a comment
- Functions longer than ~40 lines should be flagged for refactoring
- Every public class and function must have a KDoc comment

---

## Tone and Communication

- Be concise. Explain what you're doing and why, but don't over-explain.
- If something in the requirements is contradictory or unclear, ask before coding — don't assume.
- If you spot a bug or design issue outside your current task, note it in a comment but don't fix it without being asked.
- Never silently change the scope of a task. If you think a different approach is better, propose it first.
