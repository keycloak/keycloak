---
name: kc-code-review-threat-modeling
description: Runs a Microsoft STRIDE threat model analysis on a change set (commits, PR, uncommitted changes, or a specific Keycloak feature)
plan: true
---

# Purpose

You are a security architect planning a STRIDE threat model analysis of a codebase.

Apply the Microsoft STRIDE threat modeling framework to a change set in the Keycloak codebase. Produces a structured 
report covering all six STRIDE categories with severity ratings and mitigation status.

## Instructions

Run these steps sequentially.

1. **Resolve the change set.** Determine the target using the first matching rule:
   - If the user provided a PR number or URL: fetch the diff with `gh pr diff <number>` and the PR description with `gh pr view <number>`
   - If the user said "uncommitted", "staged", or "working tree": use `git diff` and `git diff --cached`
   - If the user named a Keycloak feature or area (e.g. "organizations", "identity brokering"): scope the analysis to that area's source files (this is a targeted review, not a diff)
   - Otherwise: use `git diff upstream/main...HEAD` to get the diff of the current branch vs upstream
   - If the source is ambiguous, ask the user to clarify

2. **Read the change set and load DFD context.** Read the diff or relevant source files. Then:
   - If the file `.agents/<team>/<team>-reference-architecture.md` exists, read it. Use the **Area DFD Files** index to find the relevant DFD file(s) by area label. Then read each DFD file and consult its **Components** table to confirm the affected source files map to that area. Load the diagrams, data flows, and threat targets from the relevant DFD files.
   - If the DFD file does not exist, note "DFD reference unavailable" and continue with manual identification. A new DFD will be created in the DFD review step (step 6).
   - Review and identify:
     - Which actors are involved when using the affected components
     - Trust boundaries crossed — use the DFD trust boundary annotations if available, otherwise use general categories (e.g. user input to server, server to database, inter-service calls, admin API, token endpoints)
     - Data flows — use the DFD data flow annotations if available (authentication data, tokens, user attributes, configuration, secrets)
     - Which assets are exposed or modified by the affected components
     - Which threat targets are relevant to the affected components

3. **CVE cross-reference.** If the file `.agents/memory/reference_keycloak_cves.md` exists:
   - Read it and filter CVEs relevant to the areas touched by the change set
   - Keep the filtered CVEs for step 5
   - If the file does not exist, note "CVE memory file unavailable" and continue

4. **STRIDE analysis.** For each of the six STRIDE categories, analyze the source code through that specific lens. **CRITICAL: Perform the analysis independently from the DFD** — read the code, trace data flows, and identify threats based on what the code actually does, NOT based on what the DFD already documents. The DFD is a reference for comparison in the cross-reference sub-step below, not the source of truth. The source code is the source of truth. This prevents confirmation bias where the analysis only validates existing DFD entries and misses undocumented threats.

   Use the Keycloak-specific focus areas below:

   - **Spoofing:** Authentication bypass, session hijacking, identity impersonation, forged tokens, SSO trust boundary violations, broker identity confusion
   - **Tampering:** Unauthorized data modification, parameter manipulation, request forgery, missing input validation, integrity check gaps, model attribute injection
   - **Repudiation:** Missing audit events, insufficient logging, actions not attributable to a user, missing admin event logging, untracked configuration changes
   - **Information Disclosure:** Sensitive data in responses or logs, error messages leaking internals, missing access control on data endpoints, credential exposure, token leakage
   - **Denial of Service:** Unbounded loops or queries, missing rate limiting, resource exhaustion, cache poisoning, unbounded cache entries, missing pagination
   - **Elevation of Privilege:** Missing authorization checks, IDOR, role or permission bypass, cross-realm access, admin API access control gaps, FGAP bypass, organization boundary violations

   For each threat found, record: the affected component (file or class), severity (Critical / High / Medium / Low / Informational), mitigation status (Mitigated / Partial / Missing), and a brief note explaining the threat. These findings feed into the Threat Targets mitigation statuses reported in step 6.

   When the scope is a diff (PR, branch, uncommitted changes): only flag threats where the change set introduces, exposes, or fails to mitigate the risk. Do not flag speculative or theoretical threats unrelated to the actual code changes. When the scope is a feature-level review (area or feature name): the entire feature's source code is in scope — analyze all code paths for threats regardless of whether the DFD already documents them.

   **Cross-reference with DFD (after independent analysis).** If DFD data was loaded in step 2, compare your independently discovered threats against the DFD-documented threat targets:
   - Identify threats you found that the DFD does NOT document — these are new `Add` entries for step 6.
   - Identify DFD-documented threats that your analysis confirms — these become `Confirm` entries.
   - Identify DFD-documented threats whose mitigation status or description no longer matches the code — these become `Update` entries.
   - Flag any DFD-documented threats that the code fails to address.

5. **Cross-reference with CVEs.** If CVE data was loaded in step 3, check whether any identified threats match patterns from known Keycloak CVEs. Annotate matching threats with the CVE ID.

6. **Review the DFD document.** Using the STRIDE findings from step 4 and the code read in step 2, systematically review each section of the DFD for accuracy. Use `dfd/TEMPLATE.md` as the reference template for the expected structure and level of detail.

   If no DFD exists for the area under analysis, create a new one from scratch using the `dfd/TEMPLATE.md` template structure and populate it with the findings from the analysis.

   For each section below, compare the DFD content against what the analysis discovered and classify each finding as **Add** (new item), **Update** (existing item needs modification), or **Remove** (stale/invalid item). If a section is accurate, skip it — only report sections that need changes.

   - **Assumptions:** Check for new assumptions surfaced during analysis (e.g. a security property the code relies on but the DFD does not document). Check for existing assumptions invalidated by code changes.
   - **Actors:** Check for new actor types discovered in the code that are not listed. Check for existing actors whose trust level or description is inaccurate.
   - **Diagrams (mermaid):** Only suggest changes for structural reasons — a new trust boundary was introduced, a new actor was added, or a component was entirely added or removed from the flow. Do NOT suggest diagram changes for minor flow adjustments, renamed methods, or cosmetic improvements. If the existing diagram already conveys the correct actors, trust boundaries, and data flow, report no changes.
   - **Entry Points:** Check for new endpoints or authentication gates added in code. Check for removed or renamed endpoints still listed. Check for changed trust levels.
   - **Exit Points:** Check for new response types or data exposure paths. Check for existing exit points with changed threat relevance based on STRIDE findings.
   - **Assets:** Check for new protected resources. Check for removed assets or changed access control requirements.
   - **Threat Targets:** For **every** threat target in the DFD, report the current mitigation status (`Mitigated`, `Partial`, or `Missing`) with a brief justification citing the code. Group entries by target category using sub-headers (e.g. `#### Target TT-1: Authentication & session security`). **CRITICAL: Do not limit the analysis to existing DFD entries.** The independent STRIDE analysis from step 4 is the primary source of threats — every threat discovered in step 4 that is not already documented as a TT-N.M entry MUST be reported as an `Add` action with a new TT identifier. Finding new threats is equally important as confirming existing ones; the DFD is a living document that grows with each analysis. Check for existing threats whose risk level, STRIDE category, or mitigation status should change — report these as `Update` actions. Check for threats that are no longer relevant — report as `Remove` actions.
   - **Components:** Check for new classes or files involved in the area that are not listed. Check for renamed or removed classes still listed. Check for incorrect source paths.

7. **Generate the report** using the format from the Report section below. Include an **Analysis Notes** section at the end with narrative commentary on the most significant findings — status changes, new threat context, priority guidance, and cross-cutting observations.

## Report

```
## Summary

Change set: <description of the change set source — e.g. "PR #1234", "branch cve-455 vs upstream/main", "uncommitted changes">
Date: <YYYY-MM-DD>
Components: <comma-separated list of affected modules/components>
DFD Context: <comma-separated list of relevant DFD diagrams consulted, e.g. "2.1 OIDC Authorization Code Flow, 2.4 Admin API Flow" — or "DFD reference unavailable">

Total threats: <count>
By severity: Critical: <n>, High: <n>, Medium: <n>, Low: <n>, Informational: <n>
By mitigation: Mitigated: <n>, Partial: <n>, Missing: <n>

## DFD Update Suggestions

DFD file: <path to the DFD file reviewed, or "New DFD — no file exists yet" if creating one>

<if the DFD was consulted and no updates are needed, write "DFD is up to date — no changes suggested.">

<if no DFD existed and a new one was created, write "DFD reference unavailable — a new DFD was created." and include the full DFD content below following the dfd/TEMPLATE.md template structure (frontmatter, assumptions, actors, diagrams, entry points, exit points, assets, threat targets, components).>

IMPORTANT: Only include subsections that have changes. OMIT any subsection entirely (no header, no table, no "No changes." text) when it has no updates. For example, if only Threat Targets and Exit Points have changes, output ONLY those two subsections.

Each subsection that IS included MUST use the table format shown below for that section type.

### Assumptions (only if changes exist)

| Action | Item | Detail |
|--------|------|--------|
| <Add / Update / Remove> | <assumption identifier, e.g. "A7"> | <what should change and why> |

### Actors (only if changes exist)

| Action | Item | Detail |
|--------|------|--------|
| <Add / Update / Remove> | <actor identifier, e.g. "Actor: BulkClient"> | <what should change and why> |

### Diagrams (only if changes exist — structural reasons only)

| Action | Item | Detail |
|--------|------|--------|
| <Add / Update / Remove> | <diagram element, e.g. "TB5: New trust boundary"> | <structural change — new trust boundary, actor, or component added/removed> |

### Entry Points (only if changes exist)

| Action | Item | Detail |
|--------|------|--------|
| <Add / Update / Remove> | <entry point identifier, e.g. "EP-12"> | <what should change and why> |

### Exit Points (only if changes exist)

| Action | Item | Detail |
|--------|------|--------|
| <Add / Update / Remove> | <exit point identifier, e.g. "EX-7"> | <what should change and why> |

### Assets (only if changes exist)

| Action | Item | Detail |
|--------|------|--------|
| <Add / Update / Remove> | <asset identifier, e.g. "AS-11"> | <what should change and why> |

### Threat Targets (only if changes exist)

Group by target category. Each category gets its own `####` sub-header and table.

#### Target TT-N: <category name>

| Action | Item | Mitigation | Detail |
|--------|------|------------|--------|
| Confirm | <TT-N.M> | <Mitigated / Partial / Missing> | <brief justification citing the code location or mechanism> |
| Add | <TT-N.M> | <Mitigated / Partial / Missing> | <Risk level>, <STRIDE category> — <threat description with code references> |

Action values: `Confirm` = existing entry verified unchanged. `Update` = mitigation status or description changed. `Add` = new threat not in the DFD (Detail cell MUST start with risk level and STRIDE category, e.g. "Medium, Tampering — ..."). `Remove` = stale entry.

### Components (only if changes exist)

| Action | Item | Detail |
|--------|------|--------|
| <Add / Update / Remove> | <component name> | <what should change — new class, renamed path, removed file> |

## CVE Cross-Reference

<if CVE data was available, list any matches between identified threats and known Keycloak CVEs. If no matches, write "No matches with known CVEs.">
<if CVE data was unavailable, write "CVE memory file unavailable — cross-reference skipped.">

## Analysis Notes

<Narrative commentary on the most significant findings from the analysis. Include:
- Status changes: threats whose mitigation status changed (e.g. Partial → Mitigated) with explanation of what changed and why the new status is justified
- New threats: deeper context on newly discovered threats — the attack scenario, why it matters, and any nuance not captured in the table's Detail cell
- Priority guidance: which Missing or Partial threats are highest priority and why
- Cross-cutting observations: patterns or systemic issues that span multiple threat targets

Keep each note to 2-4 sentences. Use threat IDs (TT-N.M) to link back to the tables. This section is for context that helps the reader prioritize and act — not for restating what the tables already say.>
```

## **CRITICAL Rules**

### Report format

- **The Report section above is the ONLY valid output format.** Copy its structure exactly — same headers, same table columns, same row patterns. Any deviation is a format violation.
- **Every DFD Update Suggestion subsection MUST use a markdown table.** No prose, no bullet lists, no paragraphs — only `| col | col |` tables. If a subsection has no changes, OMIT the subsection entirely (no header, no text).
- **Table cells MUST be short enough to render in a terminal.** Keep the Detail cell under 200 characters. If a threat description is longer, summarize — the full detail belongs in the DFD file, not the report.
- **Column contents are strict:**
  - `Action`: one of `Confirm`, `Update`, `Add`, `Remove`
  - `Item`: identifier only (e.g. `TT-5.6`, `EX-8`, `A7`) — no risk level, no STRIDE category, no description
  - `Mitigation` (Threat Targets only): one of `Mitigated`, `Partial`, `Missing`
  - `Detail`: brief justification. For `Add` rows, start with `<Risk>, <STRIDE> —` then the summary
- Do not use the DFD file's own table format (`| Mitigation | # | Risk | STRIDE | Threat |`) — always use the report template format (`| Action | Item | Mitigation | Detail |`)
- Do not include per-STRIDE-category breakdown tables as separate sections. STRIDE findings feed into the Summary counts and Threat Targets tables only

### Analysis rules

- Only flag threats grounded in the actual change set — no speculative or theoretical threats
- Severity scale: Critical > High > Medium > Low > Informational
- Mitigation status values: Mitigated (threat is addressed in the code), Partial (some controls present but incomplete), Missing (no mitigation found)
- The DFD Update Suggestions section is mandatory when a DFD was consulted or when no DFD exists for the area. Suggestions must be grounded in analysis findings, not speculative
- When no DFD exists for the area under analysis, create a new DFD following the `dfd/TEMPLATE.md` template structure and include it in the DFD Update Suggestions section
- Do not suggest diagram changes unless there is a structural reason (new trust boundary, new actor, component added or removed). Diagrams are stable by default
- Do not post any GitHub comments unless explicitly asked
- **CRITICAL: Report-first protocol** — ALWAYS generate and present the full report to the user FIRST. Do NOT apply DFD update suggestions or modify any DFD files until the user explicitly asks you to apply the changes. The report is for review; the user decides whether and when to apply updates
