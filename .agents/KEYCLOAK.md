# Keycloak Rules and Guidelines

## Teams

**CRITICAL: Always check the team index documents before starting to work on an issue to understand if it is related to a specific team and to follow the guidelines for that team.**

- [core-iam](core-iam/CORE_IAM_TEAM.md) — Core-IAM index document with information about the core-iam team, its areas of responsibility, and guidelines for working on issues related to the team.
- [core-protocols](core-protocols/CORE_PROTOCOLS_TEAM.md) — Core-IAM index document with information about the core-protocols team, its areas of responsibility, and guidelines for working on issues related to the team.

## Modules

### `Admin API`

The `Admin API` is a REST management interface for managing realms and their resource types located at `services/src/main/java/org/keycloak/services/resources/admin/***`.

Whenever you are analyzing code related to the `Admin API`, you should consider the instructions from the @.claude/rules/admin-api.md file.

## Behavioral Guidelines

- Never push, force push, create PRs, or any other operation in the upstream repository without explicit user approval
- Never push, force-push, or perform destructive git operations without explicit user approval
- Ask before taking risky or irreversible actions (affecting shared systems, deleting files, etc.)
- Never share secrets or credentials
- Prioritize security - identify vulnerabilities and choose security over performance when they conflict
- Be concise and direct - lead with answers, not preamble
- Don't over-engineer - no unnecessary abstractions, error handling for impossible cases, or features beyond what's asked
- Read code before modifying it - understand existing patterns first
- Use dedicated tools (Read, Edit, Grep, Glob) instead of bash commands where applicable

## Contribution Requirements

- Each PR should have an associated GitHub issue (even for small changes)
- One feature/change per PR
- One commit per PR (use `git rebase -i` to squash)
- Include functional/integration tests
- Include documentation updates
- Sign off commits with `-s`
- Branches should be named `issue-X` where `X` is the issue number

## Build

 - Always use the Maven wrapper (`./mvnw`) to run Maven commands
 - Never run a full build running tests. To run a full build use ` ./mvnw -DskipTests -DskipTestsuite -DskipExamples -DskipAdapters -DskipDocs clean install`
 - To build a distribution use `./mvnw -pl quarkus/dist -am clean install -DskipTests`

## Documentation

- Server Admin: https://www.keycloak.org/docs/latest/server_admin/
- Authorization Services: https://www.keycloak.org/docs/latest/authorization_services/
- Themes: https://www.keycloak.org/docs/latest/server_development/
- User Storage: https://www.keycloak.org/docs/latest/server_development/

## GitHub Repositories

- Upstream: https://github.com/keycloak/keycloak
- Fork: https://github.com/<github_user>/keycloak
- Local: .

## **CRITICAL: Use project's agent for code reviews**

When asked to review code, you MUST use the kc-code-reviewer agent to perform the review as per file @.claude/agents/code-reviewer.md. This is a critical requirement to ensure that all code reviews are consistent, thorough, and adhere to the project's standards. Always follow the guidelines and steps outlined in the code-reviewer agent when conducting code reviews for this project.