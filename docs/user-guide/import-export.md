[User Guide](user-guide.md) › [Settings](settings.md) › Import and Export

# Import and Export

Settings can be transferred between devices or backed up using the Import and Export buttons in the [Settings](settings.md) screen under **Configuration**.

## Exporting

1. Tap **Export Settings**.
2. Choose **No encryption** for a plain JSON file, or **Encrypt with PIN** and enter a 4-digit PIN.
3. The system file picker opens — choose a folder and confirm. The file is saved as `ema-companion-settings.json`.

## Importing

1. Tap **Import Settings** and select a previously exported file.
2. If the file is plain JSON, settings are merged immediately.
3. If the file is encrypted, you are prompted for the PIN. Entering the wrong PIN shows an error and leaves all settings unchanged.
4. Only the fields present in the file are updated; any fields not in the file keep their current values.

A log entry is created listing which fields were imported. Sensitive values such as your App Secret are never shown in full — they appear as `[hidden]` in the log.
