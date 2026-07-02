---
name: kc-issue-fix
description: A task that fixes a GitHub issue given by the user.
---

# Purpose

When the user asks for fixing an issue, you should ask for the issue number, if not already provided, and analyze the issue
accordingly to the instructions herein defined.

## Instructions

Run in plan mode.

Run these steps sequentially without asking for input if you are not told otherwise from the step instruction.

1. Run the `kc-issue-analyze` skill to analyze the issue and generate a report first in case the issue is not already analyzed. If the issue is already analyzed, you can skip this step and use the analysis report to implement the fix.
2. If the issue is not a bug but an enhancement, stop here and ask the user for confirmation if they want to proceed with the implementation of the enhancement. If not, stop here.
3. Create branch: `git checkout -b issue-<n>` from `upstream/main`. Make sure you have fetched latest changes from `upstream/main` before creating the branch.
4. Suggest tests covering the fix
5. Suggest a fix
6. If the user confirms the fix and the tests, implement them
7. Commit and sign-off using the message format:
   ```
   <issue title>

   Closes #<n>
   ```
8. Ask the user if you should push the branch to the fork. If so, push: `git push -u origin issue-<n>`

## **CRITICAL Rules**
- Always use the latest code from upstream/main during your analysis
- NEVER refactor or change any code that is not strictly required for the fix
- NEVER provide a full report of the changes you are doing. Just report whether you finished successfully or not
- **Branch format:** `issue-<n>` (e.g. `issue-47017`)
- **Commit format:** issue title on first line, blank line, `Closes #<n>`
- Keep your changes in a single commit
- When building locally for development or tests, prefer running a full install so snapshot artifacts are produced and installed into the local Maven repository. Example (from repository root):
    - mvn -f development/keycloak/pom.xml clean install -DskipTests
- Avoid invoking the `test` goal directly for large multi-module builds because it may not build and install distribution artifacts required by other modules; use `clean install` which compiles, packages and installs artifacts for downstream consumption