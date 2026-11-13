# FINT Event Model

[![Build Status](https://jenkins.fintlabs.no/buildStatus/icon?job=FINTmodels/fint-event-model/master)](https://jenkins.fintlabs.no/job/FINTmodels/fint-event-model/master)

Event model library for FINT, targeting Java 25 and Spring Boot 4.1.2 dependency
versions. Produces a standard library JAR with Jackson 3 support.

## Build and test

Install JDK 25, set `JAVA_HOME` to that JDK, and use the included Gradle 9.9.1 wrapper:

```sh
./gradlew clean build jacocoTestReport --warning-mode=fail
./gradlew test --tests 'no.fint.event.model.SpringBootCompatibilitySpec'
```

Test reports are in `build/reports/tests/test/`; coverage reports are in
`build/reports/jacoco/test/`. `./gradlew javadoc` writes API documentation to
`javadocs/`. Build artifacts, including source and Javadoc JARs, are in `build/libs/`.

`./gradlew check` and `./gradlew build` enforce at least **95% line coverage** and
**90% branch coverage** through JaCoCo. `lombok.config` marks generated Lombok
methods so coverage measures handwritten behavior consistently across Java 8 and
Java 25. No production classes are excluded from coverage.

## Installation

After a 4.x release is published, add it to the consuming application's
`build.gradle` (replace `4.0.0` with the published version):

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri('https://repo.fintlabs.no/releases')
    }
}

dependencies {
    implementation 'no.fint:fint-event-model:4.0.0'
}
```

## Migration from 3.x

This branch requires Java 25. `EventUtil` now accepts Jackson 3
`tools.jackson.databind.ObjectMapper` and `tools.jackson.core.type.TypeReference`.
Update those imports and rebuild consumers. Jackson annotations remain in
`com.fasterxml.jackson.annotation`. Event JSON field names and the existing
null-on-JSON-error behavior are preserved; JSON property order is not guaranteed.

The library exports the Spring Boot 4.1.2 BOM as a regular Gradle platform and
does not install a logging backend. Spring Boot applications can supply their
auto-configured `JsonMapper` to `EventUtil` overloads to use application-specific
JSON configuration. The default utility mapper is independent of Spring.

See [the modernization plan](MODERNIZATION_PLAN.md) for migration steps and
validation results.

## Usage

Create new Event.

```java
Event<String> event = new Event<>("rogfk.no", "FK1", "GET_ALL", "VFS");
```

with action enum
```java
Event<String> event = new Event<>("rogfk.no", "FK1", DefaultActions.HEALTH, "test");
```


Transform Event to json.

```java
String json = EventUtil.toJson(event);
```

Transform json to Event.

```java
Event event = EventUtil.toEvent(json);
```

Convert Event data into objects.

```java
List<TestDto> eventData = EventUtil.convertEventData(event, new TypeReference<List<TestDto>>() {});
```

Import `tools.jackson.core.type.TypeReference` for the final example.

## Publishing

Release automation builds with JDK 25 and publishes tagged versions to
Reposilite. Configure `REPOSILITE_USERNAME` and `REPOSILITE_PASSWORD` as CI secrets.
The build uses `-Pversion=...` for releases and defaults to `4.0.0-SNAPSHOT` locally.
