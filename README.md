# School Manager v1.3

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
