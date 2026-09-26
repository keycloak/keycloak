# Security models

This directory contains maintainer-facing security models for security-sensitive Keycloak subsystems.

These documents define the assets, trust boundaries, attacker capabilities, security guarantees, failure assumptions, accepted limitations, and implementation invariants that contributors need to preserve. They are design and review contracts, not user documentation and not a claim that a subsystem has undergone a formal security audit.

Each security model should use the following structure where applicable:

```markdown
# <Subsystem> security model

## Scope
## Assets
## Trust boundaries
## Attacker capabilities
## Security guarantees
## Concurrency semantics
## Persistence and consistency
## Failure assumptions
## Accepted limitations
## Security-sensitive implementation invariants
## Related implementation and issues
```

Guarantees should describe externally meaningful behavior without depending on the current implementation. Implementation invariants should separately record mechanisms that currently provide those guarantees. Accepted limitations and failure assumptions should be explicit so that a future change does not accidentally strengthen or weaken the documented contract.

Security models should be updated when a change modifies a security guarantee, trust boundary, failure assumption, or security-sensitive invariant.

Available models:

- [Session security model](sessions.md)
