# Repository Guidelines

## Project Structure & Module Organization
This repository is in a bootstrap state. Currently, only `.java-version` (Java 25) is present, and no `src/` or `test/` tree exists yet.

When adding code, use this baseline layout:
- `src/main/java/...` for production code
- `src/test/java/...` for unit/integration tests
- `src/main/resources/` for runtime assets and config templates
- `docs/` for architecture notes and decision records

Keep package names lowercase and directory-aligned (example: `com.dynamis.scenegraph`).

## Cross-Repo Ownership Policy
Do not create parallel fundamental types already owned elsewhere in Dynamis.

- DynamisCore owns identity/lifecycle primitives; use `org.dynamis.core.entity.EntityId` directly.
- Vectrix owns math; use `org.vectrix.core` and `org.vectrix.affine` types (`Vector3f`, `Quaternionf`, `Matrix4f`, `Transformf`).
- DynamisSceneGraph owns hierarchy and renderer-agnostic extraction behavior (node parenting, dirty propagation, visibility/culling, render extraction).

For transforms, SceneGraph must not introduce a competing transform class. Use Vectrix `Transformf` (or TRS via Vectrix types) and compute world `Matrix4f` values as needed.

## Build, Test, and Development Commands
No build tool is configured yet (no `pom.xml` or `build.gradle` currently tracked). If you introduce one, include wrapper scripts and document commands in this file.

Until then, use repository hygiene commands:
- `git status` to verify local changes
- `git log --oneline -n 10` to review recent history
- `git diff` to inspect edits before commit

If you add Maven:
- `./mvnw clean verify` for full build + tests

If you add Gradle:
- `./gradlew clean test` for baseline verification

## Coding Style & Naming Conventions
- Use 4-space indentation for Java and config files.
- Class names: `PascalCase` (`SceneGraphBuilder`).
- Methods/fields: `camelCase` (`buildSceneGraph`).
- Constants: `UPPER_SNAKE_CASE`.
- One top-level public class per file; filename must match class name.

Adopt and commit formatter/linter config with the first source contribution (for example, Spotless or Checkstyle).

## Testing Guidelines
- Mirror production packages under `src/test/java`.
- Test class names: `<ClassName>Test`.
- Test methods should describe behavior (`createsNodeForValidInput`).
- Add regression tests for every bug fix.
- Add contract tests to prevent duplicate ID/math fundamentals in SceneGraph APIs.

Target meaningful coverage on core graph logic before broad utility code.

## Commit & Pull Request Guidelines
Current history uses short, imperative commits (example: `initial`). Continue with concise imperative messages, optionally scoped:
- `core: add scene node traversal`
- `test: cover empty graph serialization`

PRs should include:
- Purpose and summary of changes
- Linked issue (if available)
- Test evidence (command + result)
- Notes on follow-up work or known limitations
