# android-yaml-strings

Gradle plugin to maintain Android translation strings in yaml files

This graddle plugin enables to use simple to write [yaml](https://yaml.org/) files
as translation files for your Android project
by translating them, as a transparent step of your build process,
into `string.xml` resource files.

## Usage

Place you yaml translation files in `<module>/src/<sourceSet>/translations/<langcode>.yaml`.
Those files contains maps of ids to the translated string in `langcode`.

```yaml
myid: This is a translation
otherid: Translate me
multiline: |>
   This content is divided
   in multiple lines.
```

Then you can refer those strings in your code as:
`com.mydomain.myapp.R.myid`,
just like ids in `strings.xml` files.

### Configuration

Enable those repositories in settings.gradle.kts:
```kotlin
pluginManagement {
    repositories {
        mavenLocal() # If you install it locally
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
```

In your project `build.gradle` add:

```groovie
plugins {
    id 'net.canvoki.android-yaml-strings' version '1.0.0'
}

# Optional, just add it if 'en' is not you reference language
yamlToAndroidStrings {
    defaultLanguage = "en"
}
```
or in `build.gradle.kts`:

```kts
plugins {
    id("net.canvoki.android-yaml-strings") version "1.0.0"
}

# Optional, just add it if 'en' is not you reference language
yamlToAndroidStrings {
    defaultLanguage.set("en")
}
```

### Sections

You can setup sections in your yaml.
XML ids are constructed by joining the levels with a double underscore.

```yaml
section:
  child: My string
```

Renders into `section__child` id.

### Named parameters

Android strings can only have indexed parameters to interpolate in the string.
This plugin adds the ability to use named parameters in the translated strings,
like `"Hello {user}! Welcome to {appname}"`
This eases the translation task by better identify the meaning of the parameters
and change the order if the language asks for it.

Sadly, in code we are still limited to positional parameters,
so the order of the named parameters in the reference language sets the indexes to be used in the code.
Proper name correspondence are checked in compile time.

Edge cases:

- Curly braces in yaml are meaningfull, if the string starts with a parameter, you have to double quote it

```yaml
nloaded: "{n} items loaded" # prevent parsing it as inline map
nloaded2: You have 
```

- If you want a real curly brace in the string you have to double it.

```yaml
bracestring: {{this curly brace is not a parameter}}
```

Parameters also can have format specs `{name:formatspec}`.
Just the same space you would use in `strings.xml`

### Language enumeration

The string array resource with id `supported_language_codes`
contains a list of all the translated languages.

## Inner behaviour

The plugin generates intermediate `<srcSet>/res/value-<lang>/string.xml` in `build/`.
It also genereates a resource file
`<srcSet>/res/values/arrays_languages.xml`
containing the language codes of all the translations
with string array id of `supported_language_codes`
so that you can enumerate them.

## Development

```bash
# Pass tests
./gradlew test

# Install in local
./gradlew publishToMavenLocal

# Publish in plugins.gradle.org
export GRADLE_PUBLISH_KEY=xxxxxxxxxxxxxxxxxxxxxxxxx
export GRADLE_PUBLISH_SECRET=zzzzzzzzzzzzzzzzzzzzzzzzzzzz
./gradlew publish
```

