# android-yaml-strings

Gradle plugin to maintain Android translation strings in yaml files

This graddle plugin enables to use simple to write [yaml](https://yaml.org/) files
as translation files for your Android project
by translating them, as a transparent step of your build process,
into `string.xml` resource files.

## Usage

Every `app/src/main/translations/<code>.yaml` file is turned in to 
`app/src/main/res/<code>/strings.xml`.

```yaml
myid: This is a translation
otherid: Translate me
```

### Hierarchy

If your yaml has a hierarchy,
xml ids are constructed by joining the levels with a double underscore.

```yaml
parent:
  child: My string
```

Renders into `parent__child` id.

### Named parameters

Android strings may have only indexed parameters.
In yaml strings you can use `{parameternames}`, which is more flexible and less error prone.
Names will relate to indexes as their position in the reference language.
They are checked and translated to indexes in build time.

You can also add formatting directives `{name:formatspec}`.

## Maintain

./gradlew test
./gradlew publishToMavenLocal


