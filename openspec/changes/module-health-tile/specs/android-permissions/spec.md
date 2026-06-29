## ADDED Requirements

### Requirement: Declare POST_NOTIFICATIONS permission
The system SHALL declare and request `android.permission.POST_NOTIFICATIONS` at runtime on Android 13+ (API 33+) to allow posting module health alert notifications.

#### Scenario: Runtime permission requested on API 33+
- **WHEN** app first launches on Android API 33 or higher
- **THEN** system requests `POST_NOTIFICATIONS` permission via the standard runtime permission dialog
- **AND** if user denies, background checks still run but no notifications are posted

#### Scenario: No permission request needed on API <33
- **WHEN** app runs on Android API 32 or lower
- **THEN** no runtime permission is needed and notifications are posted without a dialog

### Requirement: INTERNET permission retained
The system SHALL retain the existing `android.permission.INTERNET` permission (unchanged from current app state).

#### Scenario: INTERNET permission present
- **WHEN** app builds
- **THEN** `<uses-permission android:name="android.permission.INTERNET" />` remains in AndroidManifest.xml
- **AND** all API calls continue to work
