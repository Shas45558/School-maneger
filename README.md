# School Manager

Offline-first Android school management app.

## Project

- App name: School Manager
- Package: `com.scl.mgr`
- UI: Kotlin + Jetpack Compose + Material 3
- Minimum Android: 8.0 / API 26
- Compile/Target SDK: 35
- Local database: Room
- Build: Gradle
- CI: GitHub Actions

## Working in v1

- Dashboard
- Student list
- Add student
- Edit student
- Delete student
- Search students
- Student details
- Present/Absent attendance by date
- Attendance history
- Offline local database

## Demo modules

Teachers, Classes, Subjects, Exams & Results, Fees & Payments, Notices, Events, Reports, and Settings are shown in the navigation drawer as Demo modules.

## Build locally

```bash
./gradlew assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

Push this project to GitHub. The workflow at `.github/workflows/build.yml` builds the debug APK and uploads it as an Actions artifact.

## App icon

The launcher icon is generated from the supplied `sm.png`.
