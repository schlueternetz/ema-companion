## 1. Default English String Resources

- [x] 1.1 Add all user-visible strings to `res/values/strings.xml`, including the existing `app_name` and the "Hello World!" placeholder (rename to a meaningful key when the real content is known)
- [x] 1.2 Replace the hardcoded `"Hello World!"` string in `res/layout/activity_main.xml` with the `@string/` reference

## 2. German Translations

- [x] 2.1 Create `res/values-de/strings.xml` with German translations for every string defined in step 1
- [x] 2.2 Verify all keys match between `values/strings.xml` and `values-de/strings.xml` (no missing entries)

## 3. Lint Verification

- [x] 3.1 Run `./gradlew lint` and confirm zero `HardcodedText` warnings for user-visible strings
- [x] 3.2 Run `./gradlew lint` and confirm zero `MissingTranslation` errors for the `de` locale
