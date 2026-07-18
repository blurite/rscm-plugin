# RSCM

IntelliJ support and compile-time validation for RuneScape Config Mapping references in Kotlin, Java, and TOML.

## Mappings

The mapping filename is the reference type. For example, `mappings/item.rscm`:

```properties
abyssal_whip=4151
abyssal_whip_note=4152
```

This defines `item.abyssal_whip` and `item.abyssal_whip_note`.

## IntelliJ plugin

Requires JDK 25.

### Build

```shell
./gradlew buildPlugin
```

The plugin ZIP is created at:

```text
build/distributions/rscm-plugin-2026.2.zip
```

### Install

1. Open **Settings → Plugins**.
2. Click the gear icon and select **Install Plugin from Disk**.
3. Select `build/distributions/rscm-plugin-2026.2.zip`.
4. Restart IntelliJ if prompted.
5. Search for **RSCM** in Settings and select your mappings directory.

## Compiler plugin

The standalone JAR contains the Gradle loader, annotations, and both compiler plugins.

### Export

```shell
./gradlew buildCompilerJar
```

The JAR is created at:

```text
build/distributions/rscm-compiler-2026.2.jar
```

Copy it into the consuming project, for example:

```text
consumer-project/gradle/rscm-compiler-2026.2.jar
```

### Kotlin

Add this to the consumer's `build.gradle.kts`:

```kotlin
import io.blurite.rscm.gradle.RscmGradleExtension

buildscript {
    dependencies {
        classpath(files("gradle/rscm-compiler-2026.2.jar"))
    }
}

plugins {
    kotlin("jvm") version "2.4.0"
}

apply(plugin = "io.blurite.rscm.compiler")

repositories {
    mavenCentral()
}

extensions.configure<RscmGradleExtension> {
    mappingsDirectory.set(layout.projectDirectory.dir("mappings"))
}
```

This bundle currently targets Kotlin 2.4.0.

### Java

Add this to the consumer's `build.gradle.kts`:

```kotlin
import io.blurite.rscm.gradle.RscmGradleExtension

buildscript {
    dependencies {
        classpath(files("gradle/rscm-compiler-2026.2.jar"))
    }
}

plugins {
    java
}

apply(plugin = "io.blurite.rscm.compiler")

extensions.configure<RscmGradleExtension> {
    mappingsDirectory.set(layout.projectDirectory.dir("mappings"))
}
```

Java compilation requires javac from JDK 17 or newer. Direct unresolved literals fail `compileKotlin` or `compileJava`; dynamic/interpolated strings are ignored.

### Optional annotations

The bundled Gradle loader adds the annotations automatically.

Kotlin:

```kotlin
import io.blurite.rscm.annotations.Rscm
import io.blurite.rscm.annotations.RscmIgnore

fun load(@Rscm("item") reference: String) = reference

@RscmIgnore
val externallyValidated = "item.not_in_the_mapping"
```

Java:

```java
import io.blurite.rscm.annotations.Rscm;
import io.blurite.rscm.annotations.RscmIgnore;

final class Loader {
    static String load(@Rscm("item") String reference) {
        return reference;
    }

    @RscmIgnore
    String externallyValidated = "item.not_in_the_mapping";
}
```

`@Rscm("item")` requires direct literals to use the `item` mapping. `@RscmIgnore` disables validation for that field, property, local variable, or parameter.

## Advanced mappings

- Child reference: configure `component=interface` in RSCM Settings so `component.bank:universe` refers to `interface.bank`.
- File reference: add `item=items` to `directory.conf` to associate item mappings with `items/*.toml`.
- Custom file extension: use `jingle=jingles|dat` to associate jingle mappings with `jingles/*.dat`.

## Features

- Syntax highlighting and completion
- Go to declaration and find usages
- Rename and safe delete
- Quick documentation
- Kotlin and Java compiler validation
- TOML support

## Credits

- [ushort](https://github.com/ushort) (Chris)
- [z-kris](https://github.com/z-kris) (Kris)
- [notmeta](https://github.com/notmeta) (Corey)
