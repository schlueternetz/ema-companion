## Purpose

Adds a footer with the developer's support links to every module-health alert email, so recipients have a path to support the project from the emails they already receive.

## Requirements

### Requirement: Module-health alert emails include a support-links footer
Every module-health alert email (YELLOW, RED, and GREEN templates) SHALL include a footer containing the developer's Buy Me a Coffee URL (`https://buymeacoffee.com/schlueternetz`) and website URL (`https://www.schlueternetz.com`), in addition to the template's existing content.

#### Scenario: YELLOW alert email includes the footer
- **WHEN** a YELLOW-status alert email body is built
- **THEN** the body SHALL contain both the Buy Me a Coffee URL and the website URL

#### Scenario: RED alert email includes the footer
- **WHEN** a RED-status alert email body is built
- **THEN** the body SHALL contain both the Buy Me a Coffee URL and the website URL

#### Scenario: GREEN (recovery) alert email includes the footer
- **WHEN** a GREEN-status alert email body is built
- **THEN** the body SHALL contain both the Buy Me a Coffee URL and the website URL
