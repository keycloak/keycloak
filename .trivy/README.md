## Scanning dependency licenses with Trivy

Distributed components of Keycloak must only include [CNCF approved licenses](https://github.com/cncf/foundation/blob/main/policies-guidance/allowed-third-party-license-policy.md), or a [license exception](https://github.com/cncf/foundation/issues/new?template=license-exception-request.yaml) is required.

Prior to submitting a license exception request review the dependency to check if it is available under multiple licenses, including one of the approved licenses.

Note: Eclipse Distribution License - v 1.0 (EDL) is based on BSD-3-Clause, which is an approved CNCF license

### Scanning Java dependencies

```bash
trivy fs --scanners license --ignore-policy .trivy/cncf-approved-licenses.rego quarkus/deployment/
```

### Scanning PNPM dependencies

```bash
pnpm install
trivy fs --scanners license --ignore-policy ../.trivy/cncf-approved-licenses.rego .
```
