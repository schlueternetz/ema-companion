## ADDED Requirements

### Requirement: Add module health tile to Home screen layout
The system SHALL add a new module health status tile to the Home screen below the existing production tile.

#### Scenario: Module health tile appears on Home screen
- **WHEN** user navigates to Home screen
- **THEN** module health tile is visible below the current production tile
- **AND** tile displays the current module health status (green/yellow/red)

#### Scenario: Tile is accessible on tablet and phone layouts
- **WHEN** Home screen is displayed on a tablet (landscape or portrait)
- **THEN** module health tile is visible and properly sized
- **AND** tile is accessible on phone layouts as well
