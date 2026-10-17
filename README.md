# GymPilot — Android

Native Android app for a small-gym management SaaS, built with Kotlin and Jetpack Compose.

## Current beta features
- Dashboard counts active memberships by start and expiry dates
- Member directory with search, duplicate-phone validation, add-member form, and member profiles
- Editable plan names, durations, and fees
- Dated attendance records with undo for today's check-in
- Fee reminders for overdue and upcoming fees
- Payment confirmation with payment method, date, amount, and per-member history
- Local persistence using Android SharedPreferences (JSON)
- Empty states and dynamic dashboard date

## Requirements
- Android Studio (current stable)
- JDK 17+
- Android SDK Platform 35

## Run
Open this repository in Android Studio, wait for Gradle sync, select the `app` run configuration, and launch on an emulator or Android device.

## Tests
Run `./gradlew testDebugUnitTest` for business-logic tests. GitHub Actions runs the tests and assembles a debug APK on pushes to `main`.

## Important limitations
This beta stores data locally on each device. Android and iPhone do not yet share data. A secure backend with authentication and gym-level data isolation is required before production use. Sample members and sample transactions are seeded on first launch.

Do not commit API keys, signing keys, passwords, or real customer data.
