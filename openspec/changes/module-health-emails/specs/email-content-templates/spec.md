## ADDED Requirements

### Requirement: Localized email subject and body templates
The system SHALL generate email content in the user's preferred language (English or German), aligned with in-app tile and notification text.

Subjects mirror the in-app status strings with an "EMA Companion: " prefix. Body text follows the in-app notification text, adds the per-module offline list, and closes with a call to action.

---

#### Email template: YELLOW alert (English)

- **Subject**: `EMA Companion: Solar module offline`
- **Body**:
  ```
  One or more solar modules in your array have not produced for 1–2 days.

  {module list — one line per module}
  Module {uid}: No production for 1 day
  Module {uid}: No production for 2 days

  Open EMA Companion to view details.
  ```

#### Email template: YELLOW alert (German)

- **Subject**: `EMA Companion: Solarmodul offline`
- **Body**:
  ```
  Ein oder mehrere Solarmodule in deiner Anlage haben seit 1–2 Tagen nicht produziert.

  {Modulliste — eine Zeile pro Modul}
  Modul {uid}: Keine Produktion seit 1 Tag
  Modul {uid}: Keine Produktion seit 2 Tagen

  EMA Companion öffnen für Details.
  ```

---

#### Email template: RED alert (English)

- **Subject**: `EMA Companion: Solar module offline — action needed`
- **Body**:
  ```
  One or more solar modules have not produced for 3 or more days. Check your array.

  {module list}
  Module {uid}: No production for 3 days

  Open EMA Companion to view details.
  ```

#### Email template: RED alert (German)

- **Subject**: `EMA Companion: Solarmodul offline – Handlungsbedarf`
- **Body**:
  ```
  Ein oder mehrere Solarmodule haben seit 3 oder mehr Tagen nicht produziert. Anlage prüfen.

  {Modulliste}
  Modul {uid}: Keine Produktion seit 3 Tagen

  EMA Companion öffnen für Details.
  ```

---

#### Email template: Recovery / GREEN (English)

- **Subject**: `EMA Companion: All modules producing`
- **Body**:
  ```
  All solar modules in your array are producing again.
  ```

#### Email template: Recovery / GREEN (German)

- **Subject**: `EMA Companion: Alle Module produzieren`
- **Body**:
  ```
  Alle Solarmodule deiner Anlage produzieren wieder.
  ```

---

### Requirement: Email content personalization
The system SHALL include per-module offline details sorted by duration (longest first), reusing the same duration strings as the in-app detail dialog.

#### Scenario: Module list formatted
- **WHEN** email is generated for YELLOW or RED alert
- **THEN** each offline module appears on its own line: `Module {uid}: No production for {n} day(s)`
- **AND** modules are sorted by `offlineDays` descending (longest offline first)
- **AND** singular/plural follows the same rule as `home_module_health_offline_singular` / `home_module_health_offline_plural`

#### Scenario: Recovery email has no module list
- **WHEN** email is generated for GREEN recovery
- **THEN** body is a single sentence — no module list

### Requirement: Email format
- Plain text only — no HTML
- Line breaks separate sections (blank line between intro, module list, and call to action)
- Emoji in subject only if present in in-app notification strings (currently none — do not add)
