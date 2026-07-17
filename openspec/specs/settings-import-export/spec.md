## Purpose

Lets the user back up and restore all app settings as a JSON file, optionally PIN-encrypted, so configuration survives a reinstall or can be shared across devices.

## Requirements

### Requirement: Settings can be imported from a JSON file
The app SHALL allow the user to import all settings from a JSON file selected via the system file picker. All 17 settings SHALL be included: `emaAppId`, `emaAppSecret`, `emaSystemId`, `emaEcuId`, `systemCapacity`, `historicDataDays`, `apiRequestLimit`, `language`, `displayMode`, `notificationsEnabled`, `baseUrl`, and the six tile/widget enabled flags (one per Home tile and one per widget). Import is a merge: fields present in the JSON overwrite the stored value; absent fields are left unchanged. The Import button SHALL be in the "API Settings" section.

The app SHALL auto-detect whether the selected file is encrypted: if the file content is valid JSON it is treated as unencrypted; otherwise it is treated as PIN-encrypted and the user is prompted for the 4-digit PIN used during export.

#### Scenario: Successful import of an unencrypted file
- **WHEN** the user taps Import and selects a plain JSON file
- **THEN** all recognized fields SHALL overwrite the corresponding stored settings without prompting for a PIN
- **AND** the Settings screen SHALL refresh to show the new values

#### Scenario: Successful import of a PIN-encrypted file
- **WHEN** the user taps Import, selects an encrypted file, and enters the correct 4-digit PIN
- **THEN** the file SHALL be decrypted and all recognized fields SHALL overwrite the corresponding stored settings
- **AND** the Settings screen SHALL refresh to show the new values

#### Scenario: Wrong PIN on import shows an error
- **WHEN** the user enters an incorrect PIN for an encrypted file
- **THEN** the app SHALL display an error message and leave all settings unchanged

#### Scenario: Missing keys in import file leave existing values unchanged
- **WHEN** the imported JSON omits some settings keys
- **THEN** the omitted settings SHALL retain their previous values

#### Scenario: Import that completes configuration unlocks navigation
- **WHEN** the app started unconfigured and an import fills in all required fields
- **THEN** the Settings screen refresh SHALL re-evaluate the configured state and enable all bottom navigation items
- **AND** the user SHALL be able to navigate to Home without restarting the app

#### Scenario: Malformed or unreadable file shows an error
- **WHEN** the selected file cannot be read, is not valid JSON, and cannot be decrypted
- **THEN** the app SHALL display an error message and leave all settings unchanged

#### Scenario: Import of tile/widget flags updates Home and widget visibility
- **WHEN** an imported file sets one or more tile/widget enabled flags to disabled
- **THEN** the corresponding tiles SHALL be hidden from Home and the corresponding widgets SHALL show their disabled message after the import completes

### Requirement: Settings can be exported to a JSON file with optional PIN encryption
The app SHALL allow the user to export all 17 settings to a user-chosen file. Before writing, the app SHALL present a dialog offering two options: export unencrypted (plain JSON) or export with a 4-digit PIN. The default suggested filename SHALL be `ema-companion-settings.json`. All 17 settings — including the six tile/widget enabled flags — SHALL always be written regardless of encryption choice. The Export button SHALL be in the "API Settings" section.

When PIN encryption is chosen, the file format SHALL be `base64(IV + ciphertext)` where the encryption key is `SHA-256(PIN)` and the cipher is AES-256-GCM. The IV SHALL be randomly generated per export. No keyfiles or device-specific keys are involved; the PIN alone is sufficient to decrypt the file on any device.

#### Scenario: Unencrypted export writes plain JSON
- **WHEN** the user taps Export, chooses "No encryption", and selects a destination
- **THEN** a plain JSON file SHALL be written containing all 17 settings keys

#### Scenario: Encrypted export writes opaque ciphertext
- **WHEN** the user taps Export, chooses "Encrypt with PIN", enters a 4-digit PIN, and selects a destination
- **THEN** a file SHALL be written whose content is base64-encoded and not human-readable
- **AND** the app SHALL display a success confirmation

#### Scenario: Export failure shows an error
- **WHEN** the file cannot be written (e.g. permission denied, storage full)
- **THEN** the app SHALL display an error message and no partial file SHALL remain
