SchoolManager v1.37 — KSP MissingType build fix

Changes:
- Added an explicit androidx.sqlite:sqlite:2.4.0 dependency so KSP can resolve Room's SupportSQLiteDatabase/migration API types in CI.
- Updated Android versionCode/versionName to 16 / 1.37.

Validation: ZIP integrity checked. A full Android build was not run in this environment because the Gradle wrapper JAR is not included.
