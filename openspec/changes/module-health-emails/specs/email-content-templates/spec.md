## ADDED Requirements

### Requirement: Localized email subject and body templates
The system SHALL generate email content in the user's preferred language (English or German).

#### Scenario: Email templates read from strings.xml
- **WHEN** email is about to be sent
- **THEN** system reads localized strings from strings.xml based on user's app language preference
- **AND** templates for all status change types are available:
  - YELLOW alert subject and body
  - RED alert subject and body
  - RECOVERY (return to GREEN) subject and body
  - Partial recovery (RED→YELLOW) subject and body (optional)

#### Scenario: Yellow alert email template (English)
- **WHEN** status changes to YELLOW
- **THEN** email uses template:
  - Subject: "⚠️ Module Alert: %d module(s) offline"
  - Body: "The following modules have not produced power:
    - Module X: offline for %d hours
    - Module Y: offline for %d hours
    
    Check your system in the EMA Companion app or the official EMA app for more details."

#### Scenario: Yellow alert email template (German)
- **WHEN** status changes to YELLOW and user language is German
- **THEN** email uses German template (translated content, same structure)
- **AND** subject and body are in German

#### Scenario: Red alert email template (English)
- **WHEN** status changes to RED
- **THEN** email uses template:
  - Subject: "🚨 Module Critical: %d module(s) offline >72 hours"
  - Body: "⚠️ URGENT: One or more modules have not produced power for more than 72 hours.
    - Module X: offline for %d hours
    
    This may indicate a hardware failure. Please inspect your system immediately.
    Open EMA Companion app or the official EMA app to view production status."

#### Scenario: Red alert email template (German)
- **WHEN** status changes to RED and user language is German
- **THEN** email uses German translation (subject and body in German)

#### Scenario: Recovery email template (English)
- **WHEN** status returns to GREEN
- **THEN** email uses template:
  - Subject: "✅ All Modules Online"
  - Body: "Good news! All modules have recovered and are producing power again.
    Your system is back to normal operation."

#### Scenario: Recovery email template (German)
- **WHEN** status changes to GREEN and user language is German
- **THEN** email uses German translation

### Requirement: Email content personalization
The system SHALL include user-specific module details in email body.

#### Scenario: Offline module details included
- **WHEN** email is generated
- **THEN** body includes for each offline module:
  - Module ID (e.g., "Module 1", "Modul 1" in German)
  - Time offline in human-readable format (e.g., "25 hours")
  - (Optional) Last known production timestamp

#### Scenario: System context included
- **WHEN** email is generated
- **THEN** body includes:
  - System ID or name (if available)
  - Link or instructions to open EMA Companion app
  - Timestamp of alert (e.g., "Alert sent at 2026-06-18 14:30")

### Requirement: Email rendering and formatting
The system SHALL ensure emails render correctly in common email clients.

#### Scenario: Plain text or HTML format
- **WHEN** email is sent
- **THEN** email is sent in a format that renders correctly in Gmail, Outlook, Apple Mail
- **AND** includes line breaks, emoji (✅, ⚠️, 🚨) for quick visual scan
- **AND** avoids complex HTML that may not render

#### Scenario: Multi-line module list
- **WHEN** multiple modules are offline
- **THEN** each module is on its own line with clear formatting
- **AND** sorted by offline duration (longest first) for priority
