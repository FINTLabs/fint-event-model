# Spring Boot 4.1.2 and Java 25 Modernization Plan

## Goal and scope

Make `fint-event-model` usable by applications running Spring Boot 4.1.2 on
Java 25. Retain a standard Maven library JAR and the existing event JSON field
names. Spring Boot dependency management supplies compatible dependency versions;
the library itself needs no application entry point or executable Boot JAR.

## Original baseline

- The current build targets Java 8 with Gradle 4.6 and downloads shared build logic.
- Jackson 2.9.5 types appear in `EventUtil`'s public API and its tests.
- Lombok relies on implicit annotation processing; Java 25 requires explicit setup.
- Tests use legacy Spock/Groovy; CI and Jenkins also use Java 8.
- Publishing already targets Reposilite, but Jenkins passes credentials differently
  from the environment variables read by the build.

## Implementation sequence

0. **Coverage prerequisite:** verify the original Java 8 revision in an isolated
   checkout with its original dependencies. Add shared regression tests for
   optional response metadata, query replacement, null messages, the flattened
   JSON contract, and unknown-field compatibility. Mark Lombok-generated methods
   consistently and require at least 95% line and 90% branch coverage before
   accepting the migration. Keep these gates in the upgraded build's `check` task.
1. **Build:** upgrade the complete wrapper to Gradle 9.9.1, pin its distribution
   checksum, select a Java 25 toolchain and Java release level, replace removed
   Gradle APIs, and remove obsolete plugins and remote shared build logic.
2. **Dependencies:** use the Spring Boot 4.1.2 BOM through native Gradle platforms.
   Expose Jackson 3 as an API dependency, configure Lombok as a compile-only
   dependency and annotation processor, and supply SLF4J at runtime without a
   production logging backend. Use Spock 2.4 with Groovy 5 and JUnit Platform,
   plus JaCoCo 0.8.16 for Java 25 coverage.
3. **JSON:** change core/databind imports to `tools.jackson`, retain
   `com.fasterxml.jackson.annotation` annotations, create the default mapper with
   `JsonMapper`, and catch Jackson 3's unchecked `JacksonException`. Preserve
   the existing null-on-JSON-error behavior and typed payload conversion.
4. **Tests:** migrate existing specifications, replace the obsolete class-mocking
   dependency with a real serialization failure case, and test the event contract
   with Spring Boot's auto-configured Jackson 3 mapper. Cover fixture loading,
   flattened request/response properties, ignored helper fields, unknown fields,
   and caller-supplied mapper/type-reference APIs.
5. **Automation and documentation:** move build/release workflows and Jenkins to
   Java 25 and the wrapper, align publishing credentials, and refresh README
   installation, build, and migration instructions. Keep the existing AGENTS.md
   untouched as previously requested; this migration supersedes its Java 8 advice.

## Expected files

`build.gradle`, all Gradle wrapper files, `.github/workflows/CI.yaml`,
`.github/workflows/CD.yaml`, `Jenkinsfile`, `.gitattributes`, `README.md`, `EventUtil.java`,
Jackson-using specifications, and a new Spring Boot compatibility specification.
Coverage additions include `lombok.config` and `EventRegressionSpec.groovy`.

## Validation

Run on JDK 25:

```sh
./gradlew clean build jacocoTestReport --warning-mode=fail
./gradlew generatePomFileForMavenPublication generateMetadataFileForMavenPublication
./gradlew dependencies --configuration runtimeClasspath
git diff --check
```

Verify that tests are discovered, coverage is generated, the JAR contains standard
library classes with Java 25 bytecode, and publication metadata exposes Jackson 3
without Lombok, test dependencies, or a logging implementation. Do not publish a
release during validation. Record actual outcomes below after implementation.

`build` includes `check`, which now runs `jacocoTestReport` and
`jacocoTestCoverageVerification`. The thresholds apply to all production classes;
no class exclusions or artificial tests of empty enums are used to raise coverage.

## Compatibility risks and consumer actions

- This is a breaking baseline change: consumers must run Java 25. Release it as
  a new major version rather than replacing a published 3.x artifact.
- Consumers passing `ObjectMapper` or `TypeReference` to `EventUtil` must migrate
  imports to `tools.jackson`. Jackson annotations retain their existing package.
- Jackson 3 defaults and JSON property ordering can differ. Tests compare fields
  and values rather than serialized property order; applications should validate
  their own custom modules and serializers.
- The BOM is a regular, exported platform, not an enforced platform. Consumers
  can override versions, but compatibility is validated against Boot 4.1.2.
- CI hosting and release credentials require verification in their actual runners.

## References

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot dependency management](https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html)
- [Spring's Jackson 3 migration guidance](https://spring.io/blog/2025/10/07/introducing-jackson-3-support-in-spring/)
- [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- [Spock 2.4 release notes](https://spockframework.org/spock/docs/2.4/release_notes.html)
- [JaCoCo release history](https://www.jacoco.org/jacoco/trunk/doc/changes.html)

## Implementation results

### Coverage prerequisite verified against Java 8

The migration was already present in the working tree when the coverage-first
requirement arrived. To establish the original-code baseline without reverting
that work, revision `101006b28fdfdec5cdc4f7b09b7bee5bca42c12b` was exported to an
isolated temporary directory. Its unmodified build and 44 existing tests passed
using Gradle 4.6, Corretto 8u512, Jackson 2.9.5, Lombok 1.16.20, and Spock 1.1.

The same seven new `EventRegressionSpec` tests and `lombok.config` were then copied
into that directory, with coverage verification added to its build. Production
source files and dependency versions were left unchanged. The command
`./gradlew clean test jacocoTestReport jacocoTestCoverageVerification` passed.

| Verified target | Passing tests | Line coverage | Branch coverage |
| --- | ---: | ---: | ---: |
| Original Java 8 code with regression additions | 51 | 196/198 (99.0%) | 26/26 (100%) |
| Java 25 / Boot 4.1.2 migration | 54 | 196/197 (99.5%) | 26/26 (100%) |

The explicit Lombok generated-code marker makes both reports measure handwritten
behavior consistently. The original raw report included generated equals/hashCode
branches that modern Lombok marks automatically. No production classes were
excluded. Remaining missed lines are empty enum initialization, rather than
untested event handling. Both targets exceed the 95% line / 90% branch gates.

### Migration verification

Implemented and verified locally with Temurin 25.0.5:

- Gradle 9.9.1 wrapper regenerated, including its JAR, Unix/Windows launchers,
  and official distribution SHA-256. `.gitattributes` preserves launcher line endings.
- `clean build jacocoTestReport generatePomFileForMavenPublication
  generateMetadataFileForMavenPublication --warning-mode=fail` passed.
- All **54 tests passed**, including seven shared baseline regression tests and
  three Spring Boot 4.1.2 mapper compatibility tests. JaCoCo reports **99.5% line
  coverage** and **100% branch coverage**; mandatory coverage verification passes.
- The library JAR contains 13 classes with class-file major version 69 (Java 25)
  and no executable Boot layout. Source and Javadoc JARs also build successfully.
- Runtime dependencies resolve to Jackson **3.1.7**, Jackson annotations **2.21**,
  and SLF4J **2.0.20**, as managed by Boot 4.1.2. No Spring runtime or logging
  backend is added to the library; the Boot starter is test-only.
- Maven POM and Gradle module metadata include the Boot 4.1.2 BOM and the proper
  Jackson/SLF4J scopes, without Lombok or test dependencies.
- Workflow YAML parses successfully; `sh -n gradlew` and `git diff --check` pass.
- Existing incomplete Javadoc comments still emit documentation warnings;
  the build has no Gradle deprecation warnings.

The default artifact version is now `4.0.0-SNAPSHOT` to reflect the breaking Java
and Jackson baseline. No artifact was published and no CI workflow was triggered.
GitHub Actions, Jenkins, and credentials still require verification on their actual
runners. The existing `AGENTS.md` was preserved.
