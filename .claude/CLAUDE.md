@../AGENTS.md

## CLAUDE.md

## **CRITICAL: Agent Invocation Protocol**

- Before invoking ANY custom agent **Read the agent definition** from the directory where you loaded CLAUDE.md
- When a user request matches an agents from in this project and its purpose, you MUST invoke that agent IMMEDIATELY before any other action

## **CRITICAL: Skill Invocation Protocol**

- When a user request matches a skill's purpose, you MUST invoke that skill IMMEDIATELY before any other action
- DO NOT bypass skills by implementing the same functionality manually
- Skills are not optional when they match the request
- Skills exist to provide consistent, well-tested workflows
- If unsure whether to use a skill, USE THE SKILL
- Only skip a skill if the user explicitly requests a different approach