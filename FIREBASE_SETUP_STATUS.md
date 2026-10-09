# Firebase integration status (School Manager v1.38 base)

## Applied in this source package
- Added the Google Services Gradle plugin.
- Added Firebase Auth and Cloud Firestore Android dependencies.
- Added the supplied `google-services.json` for project `school-manager-b0578` and Android package `com.scl.mgr`.
- Added an app-side Admin email check for `shas45558@gmail.com` before the existing Admin flow proceeds.

## Important: cloud migration is NOT complete
The current app's repository and synchronization manager still use the local Room database and Google Drive backup flow. These edits do not yet migrate student/attendance/monthly-exam/exam-mark records to Firestore, and teacher login is still the existing local/Drive-based implementation. Do not distribute this as a completed cloud-sync build.

## Required before a secure production release
1. Implement teacher sign-in using Firebase Authentication (app User ID mapped to a non-email login identity or a trusted backend that issues Firebase custom tokens). Do not store plaintext passwords in Firestore or trust client-side password checks.
2. Implement Firestore data model and bidirectional sync for students, attendance, monthly exams, exam marks, and teacher authorization.
3. Deploy Firestore Security Rules that only permit authenticated and authorized users to access this school's data. Do not use public read/write rules.
4. Link Google Sign-In to Firebase Authentication (request an ID token, then authenticate with `GoogleAuthProvider`) and enforce the admin email allowlist in trusted rules/backend, not only UI code.
5. Build in GitHub Actions and test admin/teacher logins plus multi-device sync before release.

Firebase project: `school-manager-b0578`
Android package: `com.scl.mgr`
Admin allowlist: `shas45558@gmail.com`
