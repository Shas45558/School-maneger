# School Manager v1.2

Offline-first Android school manager.

## v1.2 attendance updates
- Attendance has class and section filters.
- Mark attendance for one class/section at a time.
- Monthly View shows each student by name and roll with monthly Present/Absent counts.
- Dashboard shows total students plus today Present and Absent cards.
- Tapping either dashboard attendance card opens class/section totals for today.
- Existing student input behavior and `sm.png` launcher icon are retained.

Package: `com.scl.mgr`
Minimum Android: 8.0 / API 26


## v1.3 Google Drive backup

The app now has a **Google Drive** drawer item. It connects a Google/Gmail account using Google Sign-In and requests only the Drive `appDataFolder` scope. The backup is private to the app and is not a normal visible Drive file.

### First connection
- If `school_manager.db` does not exist in the connected account's app-data area, the current local database is uploaded.
- If it exists, the remote database is downloaded and merged into the local database.
- After merging, the merged local database is uploaded back to Drive.

### Merge rules
- Students are matched by **Class + Section + Roll**.
- Existing local student details are kept for a matching student.
- New remote students are added locally.
- Attendance is merged by **Student + Date**; existing local attendance wins if the same record already exists.
- Deletions are not propagated; this is an additive merge designed to avoid accidental data loss.

### Google Cloud setup
Before a release build can connect to Google, register `com.scl.mgr` as an Android OAuth client in Google Cloud Console and configure the OAuth consent screen. Use the SHA-1 certificate of the APK you install (debug SHA-1 for debug builds, release SHA-1 for release builds). Enable the Google Drive API.

The app does **not** need a Gmail password and does not request Gmail message access.
