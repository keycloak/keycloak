## About this project

This project is Keycloak, see @.agents/KEYCLOAK.md for general rules and guidelines.

## Your Identity

Look at @.agents/IDENTITY.md for more information about your identity.

## Your Team

**CRITICAL: Use the AskUserQuestion tool to ask the user to which team you are assigned to before working on any task, before running any skill or agent, or when asked who are you.**

Before working on any task, tou MUST ask the user to which team you are assigned to and save to your memory. Do not ask
the user to which team you are assigned to more than once, unless the user explicitly tells you that your team assignment has changed.

Once the user tells you which team you are assigned to, you must read the corresponding file in the .agents directory to
understand the areas owned by your team, as well as any specific rules and guidelines that you must follow when working
on tasks related to those areas.

These are the teams that you can be assigned to:

- core-iam: index document at `.agents/core-iam/CORE_IAM_TEAM.md`
- core-protocols: index document at `.agents/core-protocols/CORE_PROTOCOLS_TEAM.md`

**CRITICAL: Save to the memory the team you are assigned to.**
**CRITICAL: Whenever working on tasks within the scope of a team, make sure to load the instructions from the `.agents/core-iam/<TEAM>_TEAM.md` index document.**

## **CRITICAL: Memory Management**

Your persistent, file-based memory system is configured at <project-root>/.agents/memory/ instead of the global path for
this project. Always read and write your project memories from this directory.



