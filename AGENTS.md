# Repository Guidelines

## Project Structure & Module Organization

This repository builds the FINT event-model library, not a runnable application.
Java sources live in `src/main/java/no/fint/event/`: `model/` contains events,
requests, responses, enums, and JSON helpers; `model/health/` contains health
models; `util/` contains OData query masking. Spock specifications mirror these
packages under `src/test/groovy/`. JSON fixtures belong in `src/test/resources/`.
Build configuration is in `build.gradle`; automation lives in `.github/workflows/`
and `Jenkinsfile`.

## Build, Test, and Development Commands

Use JDK 8 and the checked-in Gradle 4.6 wrapper to match CI. The build downloads
a shared dependency script from GitHub, so initial builds require network access.

- `./gradlew clean build`: compile, run tests, and assemble the library JAR in `build/libs/`.
- `./gradlew test`: run all Spock specifications.
- `./gradlew test --tests 'no.fint.event.util.ODataQueryFilterSpec'`: run one specification.
- `./gradlew test jacocoTestReport`: run tests and generate HTML/XML coverage reports under `build/reports/jacoco/`.
- `./gradlew javadoc`: generate API documentation in `javadocs/`.

## Coding Style & Naming Conventions

Use four-space indentation and Java 8-compatible syntax. Follow nearby code:
`UpperCamelCase` classes, `lowerCamelCase` methods and fields, and
`UPPER_SNAKE_CASE` constants. Keep packages under `no.fint.event`. Preserve
existing Lombok and Jackson annotation conventions, and document public API
behavior with Javadoc. No formatter or lint plugin is configured; avoid unrelated
formatting changes.

## Testing Guidelines

Use Spock's `Specification` base class, name files `*Spec.groovy`, and write
descriptive quoted feature names with `given`/`when`/`then` or `expect` blocks.
Add regression cases for changed behavior, especially JSON compatibility, null
handling, and OData filter masking. JaCoCo reports coverage; no minimum threshold
is configured. Review test results in `build/reports/tests/test/`.

## Commit & Pull Request Guidelines

Recent commits use prefixes such as `feat:`, `fix:`, `test:`, and `chore:` followed
by a concise action, for example `fix: Handle null event queries`. Keep commits
focused. Pull requests should explain the behavior change, link relevant issues,
and report validation commands and results. Call out public API or serialized
JSON changes explicitly.

## Security & Configuration

Never commit publishing credentials. Release automation supplies
`REPOSILITE_USERNAME` and `REPOSILITE_PASSWORD` through secrets and sets the
artifact version with `-Pversion=...`. Preserve query masking when changing
logging or diagnostic output.
