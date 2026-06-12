## ADDED Requirements

### Requirement: User Guide screen renders the entry Markdown file with formatting
The app SHALL include a User Guide screen that opens at `user-guide.md` in the bundled guide folder. The Markdown SHALL be rendered with formatting (headings, bold, italic, lists, code blocks, inline code) rather than shown as raw text.

#### Scenario: User Guide displays formatted content
- **WHEN** the user navigates to the User Guide screen
- **THEN** the screen SHALL display the content of `user-guide.md` with all Markdown formatting rendered correctly

#### Scenario: User Guide is scrollable
- **WHEN** the user guide content exceeds the screen height
- **THEN** the user SHALL be able to scroll vertically to read all content

### Requirement: Relative links between guide files navigate within the app
When a rendered Markdown file contains a relative link to another `.md` file in the guide folder, tapping that link SHALL navigate to the linked file within the User Guide screen. Tapping the system Back button SHALL return to the previous file.

#### Scenario: Tapping an in-guide link opens the linked file
- **WHEN** the user taps a relative `.md` link in the rendered guide
- **THEN** the linked Markdown file SHALL be rendered in the User Guide screen

#### Scenario: Back navigation returns to the previous guide file
- **WHEN** the user has followed a link to a second guide file and taps the system Back button
- **THEN** the previously viewed guide file SHALL be displayed again

#### Scenario: Back navigation from the entry page exits the User Guide
- **WHEN** the user is viewing the entry file (`user-guide/user-guide.md`) and taps the system Back button
- **THEN** the app SHALL navigate away from the User Guide to the previous destination in the back stack (e.g. Home or Settings)

#### Scenario: External links open in the system browser
- **WHEN** the user taps a link beginning with `http://` or `https://`
- **THEN** the link SHALL open in the system browser, not within the app

### Requirement: Images in the guide folder are rendered inline
Markdown files may reference images stored in the guide folder using relative paths. Those images SHALL be rendered inline at the location they appear in the text.

#### Scenario: Relative image renders in the guide
- **WHEN** a guide Markdown file contains a relative image reference
- **THEN** the image SHALL be displayed inline at its position in the rendered text

### Requirement: User guide folder is kept in sync with repository documentation via build
The app SHALL bundle the entire `docs/user-guide/` folder as assets that are automatically copied during the build process. No manual copy step SHALL be required.

#### Scenario: Assets present after build
- **WHEN** the app is built (any variant)
- **THEN** `assets/user-guide/user-guide.md` SHALL exist in the APK
- **AND** all other files from `docs/user-guide/` (Markdown and images) SHALL also be present under `assets/user-guide/`
