# Prove2Break — Authentication

## Flows

### Registration
1. User submits email + password on Android.
2. Backend validates email format/uniqueness and password strength (min length, not a check against breach lists in MVP).
3. Password hashed (bcrypt or argon2 — decided at implementation, argon2id preferred where library support is convenient) and stored; plaintext password never persisted or logged.
4. User record created; client proceeds to login flow (or auto-logs-in with returned tokens, decided at implementation).

### Login
1. User submits email + password.
2. Backend verifies password hash match.
3. On success, backend issues a JWT **access token** (short-lived, default 30 min) and a JWT **refresh token** (long-lived, default 30 days).
4. Android stores both tokens securely (never in plain SharedPreferences/logs).

### Logout
1. Android calls `/auth/logout` with the refresh token, then clears local token storage regardless of server response.
2. Backend invalidates the refresh token (blacklist/revocation table, or short-lived rotation scheme — implementation detail decided when backend is built).

**Implementation deviation:** `backend/app/api/auth.py` does not implement a `/api/auth/logout`
endpoint (only `register`/`login`/`refresh` exist — see `backend/app/schemas/auth.py`), so
`AuthRepositoryImpl.logout()` on Android is local-only: it clears the stored access/refresh tokens
and the UI routes back to Login. No server-side token revocation happens on logout yet. Add the
backend endpoint and call it from `AuthRepositoryImpl.logout()` when it exists.

## JWT Design

- **Access token:** short expiry, carries `user_id`, `exp`, `iat`; used for all authenticated API calls.
- **Refresh token:** longer expiry, used only against `/auth/refresh`; each use may rotate the refresh token (refresh token rotation) to limit replay risk.
- **Algorithm:** HMAC (HS256) for MVP simplicity, configurable via `JWT_ALGORITHM` env var so it can move to RS256 later without a code rewrite.
- Access token expiry and refresh token expiry are both configurable via env vars (`JWT_ACCESS_TOKEN_EXPIRE_MINUTES`, `JWT_REFRESH_TOKEN_EXPIRE_DAYS`).

## Secure Storage on Android

- Tokens are stored via `EncryptedSharedPreferences` (or an encrypted DataStore implementation, decided at implementation time based on current AndroidX guidance) — never plain `SharedPreferences`, never in a Room table without encryption, never logged.
- An OkHttp `Authenticator` handles 401 responses by attempting a refresh once; if refresh also fails, the app clears tokens and routes to Login.

**Implementation deviation:** the Android auth build-out (`data/auth/AuthTokenStore.kt`) stores the
access/refresh token pair in a dedicated (non-encrypted) Preferences DataStore, not
`EncryptedSharedPreferences`/an encrypted DataStore. The project does not yet depend on
`androidx.security:security-crypto`, and adding it was out of scope for this change. The store is
isolated from the app's other settings (its own DataStore file, `auth_prefs`), tokens are never
logged, and all access goes through the `TokenStore` interface — so swapping in an encrypted
implementation later only requires changing `AuthTokenStore`, not any of its callers
(`AuthInterceptor`, `AuthAuthenticator`, `AuthRepositoryImpl`). This should be revisited before a
production release.

## Password Hashing

- bcrypt or argon2id, with a per-password random salt (handled by the hashing library) and a cost factor tuned to balance security and login latency.
- Never roll a custom hashing scheme; never use unsalted or fast general-purpose hashes (MD5/SHA-256 alone) for passwords.

## Session Expiration Handling

- On access-token expiry mid-use, the client transparently refreshes and retries the original request once.
- On refresh-token expiry, the client logs the user out gracefully with a clear "please sign in again" message — no silent data loss for an in-progress focus session, since session state lives locally in Room and re-syncs once re-authenticated.

## Error Handling

- Auth errors return generic messages to the client ("invalid email or password") — never reveal whether the email exists (avoids user enumeration) for the login endpoint specifically.
- Rate limiting applied to login/register endpoints to slow brute-force attempts (see `SECURITY.md`).
- All auth failures are logged server-side with enough context to investigate abuse, but without logging submitted passwords.
