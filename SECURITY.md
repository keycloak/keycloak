# Security Policy

*This policy is based on the [CISA vulnerability disclosure policy template](https://www.cisa.gov/vulnerability-disclosure-policy-template)*

## Introduction

The Keycloak team believes that everyone, everywhere, is entitled to access to the quality information needed to mitigate security and privacy risks. We strive to protect communities of users, contributors, and partners from digital security threats. We believe an [open approach to vulnerability management](https://www.redhat.com/en/blog/red-hats-open-approach-vulnerability-management) is the best way to achieve this.

This policy supports our open approach and is intended to give security researchers clear guidelines for submitting and coordinating discovered vulnerabilities with us. In complying with this policy, you authorize CNCF to work with you to understand and resolve the issue quickly. For more details about our processes, please read the [security charter](https://www.keycloak.org/security-charter.html).

## Guidelines

* Research shared with any Keycloak representatives/individual will be reported to and managed by the Keycloak Security Response Team in order to be officially protected and coordinated.
* Access and visibility to research and all CVE related data will follow the principle of least privilege by all vendors involved.
* All parties involved must establish a reasonable amount of time to resolve the issue before a vulnerability is disclosed publicly; agree and coordinate on public disclosure dates when possible.
* Public disclosure should be prioritized on the need to keep company, government, and individual data confidential and the general public safe.
* All vendors will honor disclosure/embargo requests in good faith as long as all guidelines are met.
* NDA signatures are not required.
* Vendors involved in coordinated disclosure will remain actively involved.

Violation of these guidelines may result in the individual, or vendor, being added to a denied coordination list.

## AI-Assisted Reports

We recognize AI as a valuable tool for security research and welcome reports where AI was used to help identify vulnerabilities. However, AI-assisted reports must meet the following requirements:

* **Validate before submitting.** You are responsible for verifying that the vulnerability is real and reproducible. Unvalidated AI output submitted as-is will be rejected.
* **Disclose AI usage.** Clearly state in your report that AI tools were used in the discovery or writing of the report.
* **Understand your findings.** You must be able to explain the vulnerability, its impact, and the reproduction steps in your own words. If we follow up with questions, we expect informed answers — not further AI-generated responses pasted without review.
* **One finding per report.** Do not submit bulk or batch reports containing multiple unrelated findings. Each vulnerability must be reported individually as outlined in the reporting steps below.

Reports that are clearly unreviewed AI output — such as those containing generic descriptions, hallucinated endpoints, or findings that do not apply to Keycloak — will be rejected without further analysis.

## Scope

This policy applies to all Keycloak components and projects. Research disclosed to the project will be limited to Response Team members; however, we will assist in coordinating the disclosure of research with upstream open-source communities as needed and requested.

## Reporting a Suspected Vulnerability

Suspected vulnerabilities should be disclosed responsibly and not made public until after analysis and a fix are available. We will acknowledge your report within 7 business days and work with you to confirm the vulnerability's existence and impact. Our goal is to maintain open dialogue during the assessment and remediation process.

### Supported Versions

Depending on the severity of a vulnerability, the issue may be fixed in the current `major.minor` release of Keycloak, or for lower severity vulnerabilities or hardening in the following `major.minor` release. Refer to [https://www.keycloak.org/downloads](https://www.keycloak.org/downloads) to find the latest release.

If you are unable to regularly upgrade Keycloak, we encourage you to consider
[Red Hat build of Keycloak](https://access.redhat.com/products/red-hat-build-of-keycloak/), which offers
[long term support](https://access.redhat.com/support/policy/updates/red_hat_build_of_keycloak_notes) of specific versions of Keycloak.

### Experimental Features

While we welcome bug reports against features that are not released yet, the security team usually does not issue CVEs for experimental features. The preview state marks that the feature is mature enough to start normal security handling.

Instead, those issues will be managed as regular bugs publicly. If in doubt, report your finding via email to the security team first to clarify if it is related to an experimental feature.

### Coordinated Vulnerability Disclosure

If you are reporting known CVEs related to third-party libraries used in Keycloak, [create a new GitHub issue](https://github.com/keycloak/keycloak/issues/new/choose).

If you discover a Keycloak security vulnerability that has been accidentally disclosed publicly, notify us immediately through [keycloak-security@googlegroups.com](mailto:keycloak-security@googlegroups.com).

If you are a **security researcher** and want to report a security vulnerability in the Keycloak codebase, follow these steps:

1. Test against the [latest released version](https://www.keycloak.org/downloads) of Keycloak and include the affected version in your report.
2. Provide detailed instructions on how to reproduce the issue with a [minimal and reproducible example](https://stackoverflow.com/help/minimal-reproducible-example).
3. Show clear evidence of exploitation, preferably as pasted log output or text. Screenshots or videos may be attached if needed. We will reject reports based on static scanners or AI without a proof-of-concept.
4. Include your contact information for acknowledgements. See "Attribution Policy" below for details.
5. Submit each finding individually to allow a separate discussion thread with our triage team.
6. Submit your report as plain text in the email body. Avoid attachments; if necessary, limit them to screenshots, videos, or reproducers (e.g., scripts or configuration files). Other attachments delay our triage process and may be returned.
7. Pick a descriptive subject for the mail matching the reported finding.
8. Email your report to [keycloak-security@googlegroups.com](mailto:keycloak-security@googlegroups.com).

If you are a **user of Keycloak** and want to report a security concern, follow these steps:

1. Identify the Keycloak version affected. Ideally, verify with the [latest released version](https://www.keycloak.org/downloads) of Keycloak.
2. If available, provide detailed instructions on how to reproduce the issue with a [minimal and reproducible example](https://stackoverflow.com/help/minimal-reproducible-example).
3. If available, provide log files or screenshots.
4. Include your contact information for acknowledgements. See "Attribution Policy" below for details.
5. Submit each finding individually to allow a separate discussion thread with our triage team.
6. Submit your report as plain text in the email body. Avoid attachments; if necessary, limit them to screenshots, videos, or reproducers (e.g., scripts or configuration files). Other attachments delay our triage process and may be returned.
7. Pick a descriptive subject for the mail matching the reported finding.
8. Email your report to [keycloak-security@googlegroups.com](mailto:keycloak-security@googlegroups.com).

### Attribution Policy

We will credit reporters who informed us in private about security vulnerabilities in security advisories.

The attribution can contain the name, alias, company, group affiliation, and GitHub username of the reporter. We will not include email addresses or links.

### Bug Bounty

There is currently no active bug bounty.

## Security Scanners

Raw output from automated security scanners will **not** be accepted. These tools often report false positives and processing unvalidated reports is disruptive to project maintainers. If a security scanner identifies a potential issue, it is your responsibility to triage the finding and provide a clear proof-of-concept demonstrating how it could be exploited specifically in Keycloak.
