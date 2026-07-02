---
name: kc-issue-triage-summary
description: A task that produces a triage summary report for open issues in the Keycloak repository for the areas owned by the team you are assigned to.
---

## Instructions

Prefer executing the steps using GitHub CLI (`gh`) to fetch the issues. If told otherwise to fetch issues using HTTP or browser, do so.

**CRITICAL: You should only process issues assigned to the Keycloak team you are a member.**

1. Fetch the dashboard page at https://www.keycloak.org/dashboard/bugs
2. Look at the `Bugs per team` table and find the row with the team name
3. Follow the link from the **Triage** column (number cell) to get the exact GitHub issue list. Use the exact same filters from the link.
4. Follow the link from the **Triage Overdue** column (number cell) to get the exact GitHub issue list. Use the exact same filters from the link.

```
## Bug Triage Summary: <your-tealm-name> 
Date: YYYY-MM-DD

### 🔵 Triage

**Summary:**
Open: X
Overdue: X ⚠️

| # | Title | Area | Assignee**** |
|---|-------|------| -----|
| ⚠️ [#XXXXX](url) | Title | area1, area2 | <assignee> |
| [#XXXXX](url) | Title | area1, area2 | <assignee> |
```

## **CRITICAL Rules**
- Use the format from the `Report` section strictly as defined, do not change the format or add any additional information that is not explicitly requested in the report format.
- If the issue has no assignee, use `Unassigned` in the `Assignee` column.
- Do not process any other column from the table other than those explicitly mentioned in the instructions
- Overdue issues (⚠️) appear first in the table
- The ⚠️ icon goes to the left of the issue number in the `#` column
- Summary shows Open and Overdue counts on separate lines
- Overdue count gets ⚠️ suffix
- Do not add any notes after the issues list