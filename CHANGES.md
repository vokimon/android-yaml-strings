# Changelog

## android-yaml-strings 1.0.0 (2025-02-16)

- 💥 Maven id: net.canvoki.gradle:android-yaml-strings -> net.canvoki:android-yaml-strings
- 💥 Kotlin DSL scope: yamlToAndroidStrings -> yamlStrings
- ✨ Safe WIP translations with `yamlStrings.autoCompletedLanguages` option.
- ✨ Array resource `incomplete_languages` for the application to filter out WIP translations or warn users.

## android-yaml-strings 0.1.0 (2025-02-13)

- ✨ First version extracted from Carburoid project
- ✨ Auto apply to every source set containing a `translations` directory
- ✨ Using named parameters mapped to index using default language order
- ✨ Hierarchical ids
