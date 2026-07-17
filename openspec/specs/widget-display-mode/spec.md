## Requirements

### Requirement: Widgets follow the app's Display Mode preference
All three home-screen widgets SHALL render using the same Display Mode preference (System, Light, or Dark) the user has already set inside the app (`dark-mode` capability), rather than only following the device's ambient system theme independently of that setting.

#### Scenario: System mode follows the device theme
- **WHEN** the Display Mode preference is "System"
- **THEN** each widget SHALL render in light or dark colours matching the device's current system theme

#### Scenario: Explicit Light mode forces light widget colours
- **WHEN** the Display Mode preference is "Light", regardless of the device's system theme
- **THEN** each widget SHALL render in light colours

#### Scenario: Explicit Dark mode forces dark widget colours
- **WHEN** the Display Mode preference is "Dark", regardless of the device's system theme
- **THEN** each widget SHALL render in dark colours

#### Scenario: Changing the preference updates widget appearance
- **WHEN** the user changes the Display Mode preference in Settings
- **THEN** each widget SHALL reflect the new preference the next time it recomposes (its own periodic update, or the next foreground/background refresh trigger)
