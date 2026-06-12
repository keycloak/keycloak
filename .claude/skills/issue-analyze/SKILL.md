---
name: kc-issue-analyze
description: A task that analyzes a GitHub issue given by the user.
---

# Purpose

When the user asks for analyzing an issue, you should ask for the issue number, if not already provided, and analyze the issue
accordingly to the instructions herein defined.

## Instructions

Run in plan mode.

Prefer executing the steps using GitHub CLI (`gh`) to fetch the issues. If told otherwise to fetch issues using HTTP or browser, do so.

Run these steps sequentially without asking for input if you are not told otherwise from the step instruction.

1. Get the issue number, from the context if any issue is being analyzed, from prompt, or ask if not sure what issue number to work on
2. Fetch the issue from GitHub
3. Check version to which the issue is being reported
4. Explore relevant source files in the local repository
5. Analyze the root cause, provide a summary with at most 3 sentences on the root cause of the issue based on your analysis of the code and the issue description
6. When analysing issues from `https://github.com/keycloak/keycloak-private/issues`:
   - Read the CVE memory file at `.agents/memory/reference_keycloak_cves.md`. Determine which <team> area(s) the changed code belongs to (e.g. area/authorization-services, area/identity-brokering, etc.), then filter the CVE list to only those whose **Core-IAM area** or **CWEs** are relevant to the code under review. Use those filtered CVEs as extra signal when deciding if the issue is still valid and if was already reported.
   - Read the `Last fetched` date from the `NVD CVEs` section of the CVE memory file. If it is more than 7 days ago (or absent), use WebFetch to query the NVD API for Keycloak CVEs published in the last 30 days, filter to core-IAM areas only, update the `NVD CVEs` section with any new entries, and update the `Last fetched` date. Skip the fetch if the data is less than 7 days old.
   - After completing the analysis, record the issue in the CVE memory file at `.agents/memory/reference_keycloak_cves.md` under the `keycloak-private Issues` section. Include: issue number, title, area, CWE, CVSS (if assessed), one-line summary, and status.
7. Check if the issue can be reproduced by looking at the issue description and the `How to reproduce?` steps
8. Check the Keycloak documentation for any references that might be related to the issue being reported
9. Based on the documentation, check if the behavior being reported is expected, an enhancement, or an actual bug
10. Check if the issue is linked with a pull request. If so, stop here and provide a run an analysis on the pull request to check if it is fixing the problem and what is left to have it ready to merge
11. Generate a report based on the format from the `Report` section

## Report

```
# <issue-number>: <issue-title>  

Version: <reported-keycloak-version>
Outdated: <`true` if reported against an old release, otherwise `false`>

## Description

<issue-description>

## Analysis

<write here a summary of the analysis and the root cause without any suggestion about the fix, only an analysis on the cause>

<write here, and based on the history of the files you are analysing, also try to find any commit and their corresponding PR that might have introduced the problem. 

<write here a brief summary of the impact of the issue for users and deployments>

<if a security or hardening issue, also provide a summary on the exposure impact>

## Blast Radius

<write here a summary of the blast radius of the issue, meaning what are the other parts of the code that are affected by the issue, and what are the potential side effects of the issue>

## Security Impact

<if the issue is not from `https://github.com/keycloak/keycloak-private/issues`, write "Not a security issue" and skip this section>

<if the issue is from `https://github.com/keycloak/keycloak-private/issues`, write a summary exactly as following>

Duplicates: <if the issue is a duplicate of another issue, write the issue number here, otherwise write "None">
Related: <if the issue is related to another issue, write the issue number here, otherwise write "None">

## Criticality

<write here how you classify the issue. Values can be `blocker, important, normal, low`. Also provide a summary of your decision>
```

## **CRITICAL Rules**
- Use the format from the `Report` section strictly as defined, do not change the format or add any additional information that is not explicitly requested in the report format.
- Look at https://github.com/keycloak/keycloak/blob/main/docs/bug-triage.md to understand how to classify issues accordingly to their criticality.
- Always use the latest code from upstream/main during your analysis