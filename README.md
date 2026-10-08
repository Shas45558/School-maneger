# School Manager v1.12

Offline-first Android school manager.

## Google Drive backup

The Google Drive drawer item connects a Google account and backs up the Room/SQLite database into that account's private Drive `appDataFolder`.

### Backup security
- The database is snapshotted locally with SQLite WAL checkpointing and `VACUUM INTO`.
- Before upload, the snapshot is encrypted with **AES-256-GCM**.
- A random 16-byte salt and 12-byte GCM nonce are generated for every backup.
- The AES key is derived from a user-supplied backup password with PBKDF2-HMAC-SHA256 (600,000 iterations).
- The app does **not** save the backup password. The same password is required to restore the backup on another phone.
- The password is never uploaded to Google Drive.
- The uploaded file is named `school_manager.db.enc`.
- Older `school_manager.db` plaintext backups are recognized and migrated to the encrypted format after a successful merge.

### First connection
1. Connect the Google account.
2. Enter a backup password of at least 8 characters.
3. Tap **Continue** / **Sync now**.
4. If no backup exists, the encrypted local database is uploaded.
5. If a backup exists, it is decrypted, merged, and the merged database is encrypted and uploaded again.

### Merge rules
- Students are matched by **Class + Section + Roll**.
- Existing local student details are kept when the same student exists on both sides.
- New remote students are added locally.
- Attendance is merged by **Student + Date**.
- Existing local attendance wins when the same record already exists.
- Deletions are not propagated; the merge is additive to avoid accidental data loss.

### Google Cloud setup
Before a release build can connect to Google, register `com.scl.mgr` as an Android OAuth client in Google Cloud Console and configure the OAuth consent screen. Use the SHA-1 certificate of the APK you install (debug SHA-1 for debug builds, release SHA-1 for release builds). Enable the Google Drive API.

The app does not need a Gmail password and does not request Gmail message access.

## Google Drive backup password (v1.4)
The Google Drive backup password can now be remembered on the current device. Enable **Remember me on this device** in the backup password dialog. After a successful sync, the password is stored encrypted using Android Keystore and future syncs will use it automatically without asking again. If the password is invalid, the saved password is cleared and the app asks for it again.


## Google Drive shared database sync

- The app uses the configured shared Google Drive folder ID `1dk-R0qwoQk_97v0T_jSco5FRko4kvZmi`.
- The single shared database file is `school_manager.db`.
- The folder owner must share the folder with each Google account that should sync.
- Google sign-in is handled by Google; the app does not collect Gmail passwords.
- Auto Sync can be enabled from the Google Drive screen. It runs at startup and periodically (Android may delay background work) when an internet connection is available.
- The database is stored without app-level encryption, as requested.


## Classes 6–8 Exams & Results (v1.14)
- Exam selection: First Term, Second Term, Annual.
- Bangla 2nd Paper 50, English 2nd Paper 50, Agriculture 20, ICT 25, Oral 10.
- English 2nd Paper, Agriculture, ICT and Oral have no MCQ input.
- Oral is excluded from GPA calculation.
- Monthly Equivalent uses existing Monthly Exam records; teacher selects 2 or 3 months and the selected-month average is added to the First/Second Term display total only. Monthly Equivalent never affects GPA.
- First/Second Term GPA: calculated from that term's subject grades using the standard 5-point scale; Monthly marks do not affect GPA.
- Annual final number: (Annual Exam + First Term Exam + Second Term Exam) / 3. This prevents Monthly Equivalent from being counted twice.
- Annual GPA: calculated only from the Annual exam subject grades using the standard 5-point scale. First Term and Second Term do not affect Annual GPA. Oral remains excluded from GPA.
- Annual ranking uses Annual GPA first. If GPA is tied, the tie-break is the average total marks across First Term + Second Term + Annual; higher average ranks first.
- Legacy exam names (1st Term, 2nd Term, Annual Exam) remain readable.

## v1.17 exam calculation updates
- English 1st Paper is CQ-only (no MCQ input).
- The exam built-in keypad is kept inside the bottom area with IME-safe padding so the CQ/MCQ entry remains visible above any system IME.
- First Term and Second Term subject totals = exam subject marks + the average mark for that subject from the teacher-selected 2 or 3 monthly exams.
- Monthly-exam selection is restricted to months in the current calendar year.
- Annual GPA remains based only on Annual exam subject marks.
- Annual displayed/final combined total = average of First Term total (including monthly equivalent), Second Term total (including monthly equivalent), and Annual exam total.
- Annual rank continues to sort by Annual GPA first, then by the average of those three exam totals as the GPA tie-breaker.
