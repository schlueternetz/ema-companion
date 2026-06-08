## Context

The app is in early development with a single Activity and a placeholder "Hello World!" layout. The only existing string resource is `app_name`. As the UI grows, establishing Android's standard localization pattern now prevents technical debt and ensures all new strings are added in a translatable way from the start.

## Goals / Non-Goals

**Goals:**
- Establish the standard Android string resource pattern (`res/values/strings.xml` + `res/values-de/strings.xml`)
- Move all existing hardcoded UI strings to resource files
- Use the device's system locale automatically; fall back to English for any unsupported locale

**Non-Goals:**
- In-app language selector (device system language is the source of truth)
- Languages beyond English and German
- Locale-specific formatting of dates, numbers, or currency (covered separately if needed)
- Right-to-left layout support

## Decisions

### Android string resources over a third-party i18n library

Android's built-in `res/values[-<locale>]/strings.xml` mechanism is the platform standard. It requires no additional dependencies, is supported natively by IDE tooling (e.g., Android Studio's Translations Editor), and integrates automatically with the `Context.getString()` / layout `@string/` reference system.

Alternative considered: a library such as `phrase` (Duolingo) for runtime locale switching. Rejected because in-app language switching is out of scope and the added complexity is not justified.

### English as default locale (no `-en` qualifier folder)

Resources in `res/values/` without a locale qualifier serve as the default fallback. Placing English strings there means any unsupported locale automatically falls back to English without extra configuration.

Alternative considered: explicit `res/values-en/` folder. Rejected because it duplicates the default strings and is unnecessary when English is the intended fallback.

## Risks / Trade-offs

- **String coverage gaps** → Mitigation: lint rule `MissingTranslation` (enabled by default in Android Gradle) will flag any string present in the default file but missing from `values-de/`.
- **Plural strings and special formatting** → Mitigation: use `<plurals>` resources and `getString(R.string.foo, arg)` format arguments where needed; document the pattern in code comments for future contributors.
- **Lint noise from non-translatable strings** → Mitigation: mark strings that should never be translated (format patterns, URLs, brand names like `"APsystems"`) with `translatable="false"` in `strings.xml`. This prevents spurious `MissingTranslation` warnings and signals intent to future contributors.
- **Word-order differences in formatted strings** → Mitigation: use positional format arguments (`%1$s`, `%2$.1f`) instead of bare `%s` / `%f`. German sentences can place arguments in a different order than English, and positional args let translators reorder them without changing the Kotlin call site.

## Migration Plan

1. Add all user-visible strings to `res/values/strings.xml`
2. Add German translations to `res/values-de/strings.xml`
3. Replace any hardcoded string literals in layout XML and Kotlin with `@string/<name>` / `R.string.<name>` references
4. Verify with `./gradlew lint` — no new `MissingTranslation` or `HardcodedText` warnings
5. No rollback complexity; removing a locale folder restores previous behavior
