# Keycloak Core Agent

A [Claude Code](https://docs.anthropic.com/en/docs/claude-code) agent configuration for the Keycloak teams. 

It provides a set of skills and agents that automate common workflows such as GitHub issue analysis, issue fixing, 
triage summary reporting, and code review — all tailored to the Keycloak project and the core-iam team areas.

While this agent is mainly designed using Claude Code, the `AGENTS.md` file is structured to also be compatible with
GitHub Copilot, allowing you to choose either platform based on your preference.

## Prerequisites

- **Claude Code** CLI installed
- **GitHub CLI** (`gh`) installed and authenticated with access to [keycloak/keycloak](https://github.com/keycloak/keycloak)
- A **local Keycloak clone** with remotes configured:
  - `upstream` → `https://github.com/keycloak/keycloak`
  - `origin` → your fork (e.g. `https://github.com/<you>/keycloak`)

## Setup

3. Launch Claude Code from the Keycloak repository root:

   ```bash
   cd /path/to/keycloak
   claude
   ```

   Claude Code automatically reads `.claude/CLAUDE.md` as the entry point and loads all skills, agents, and rules.

   If you are using GitHub Copilot, the `AGENTS.md` will serve as the entry point instead, and it should consider the same skills and agents definitions.

4. Verify the setup by asking:

   ```
   Who are you?
   ```

   This triggers the `kc-who-are-you` skill and prints a summary of the loaded context, including your identity, team, skills, and agents.

## Memory Management

By default, the agent should be configured to use auto memory, which means it will retain information across sessions and runs. This allows the agent to learn and adapt over time based on your interactions.

## Repository Structure

```
core-iam-agent/
├── AGENTS.md                        # Top-level agent context (identity, team, project refs)
├── .agents/
│   ├── IDENTITY.md                  # Agent tone, communication style, constraints
│   ├── CORE_<TEAM>_TEAM.md          # Specific context for a team (e.g. core-iam, core-protocols, etc.)
│   └── KEYCLOAK.md                  # Keycloak project rules, build commands, repos
├── .claude/
│   ├── CLAUDE.md                    # Entry point — agent and skill invocation protocols
│   ├── agents/
│   │   └── code-reviewer.md         # kc-code-reviewer agent definition
│   ├── skills/
│   │   ├── issue-analyze/SKILL.md   # kc-issue-analyze
│   │   ├── issue-triage-summary/SKILL.md  # kc-issue-triage-summary-report
│   │   └── who-are-you/SKILL.md     # kc-who-are-you
│   └── rules/
│       ├── code-style.md            # WildFly code style, spotless
│       ├── security.md              # Prompt injection prevention
│       └── testing.md               # Test suite paths and run commands
```

## Usage

### Verify agent context

```
Who are you?
```

Triggers the `kc-who-are-you` skill, and prints a summary of the loaded configuration: environment, project, tealm, role, 
constraints, available skills, and agents. 

Use this to make sure you are prompt to select one team and confirm everything is wired correctly.

### Generate a triage summary

```
Generate a triage summary
```

Fetches the current triage and overdue bug counts from the [Keycloak dashboard](https://www.keycloak.org/dashboard/bugs) and produces a formatted summary table scoped to core-iam areas.

### Analyze an issue

```
Analyze issue <#>
```

Fetches the issue `#` from GitHub, explores relevant source files, checks documentation, and produces a structured analysis report with root cause, criticality, and impact.

### Review code

Triggers the `kc-code-reviewer` agent, which launches parallel sub-agents to check for project guideline compliance, bugs, and regressions — filtering for high-signal issues only.

```
Review code
```

It will check uncommited changes and review them. If there are no uncommited changes, it will review the last commit.

or:

```
Review the code <PR>
```

It will check the change set from the PR and review it.

or:

```
Review the code from the last commit
```

It will check the change set from the last commit and review it.

## Sandboxing

Instructions on how to sandbox this agent in addition to the built-in sandboxing provided by Claude Code.

### Using `nono`

[nono](https://github.com/always-further/nono) is a capability-based sandbox that restricts filesystem, network, 
and command access for any process. It ships with a built-in `claude-code` profile that already covers Claude Code's 
own runtime needs (e.g. `~/.claude`, node, caches). You then layer on project-specific grants for the paths Claude needs to read or write.

#### Install `nono`

See https://nono.sh/docs/cli/getting_started/installation.

#### Create a launcher script

Create a script (e.g. `~/bin/sclaude`) and make it executable (`chmod +x ~/bin/sclaude`). Run this script from the root of your Keycloak clone instead of running `claude` directly.

```bash
#!/usr/bin/env bash

# Let Claude Code discover CLAUDE.md from symlinked directories
export CLAUDE_CODE_ADDITIONAL_DIRECTORIES_CLAUDE_MD=1

# Enables auto memory
export CLAUDE_CODE_DISABLE_AUTO_MEMORY=0

M2_LOCAL_REPO=/path/to/maven/.m2
CURRENT_DIR_NAME=$(pwd | sed 's|/|-|g')

nono run \
    --profile claude-code \
    --allow-cwd \
    --allow $M2_LOCAL_REPO \
    --allow /path/to/java/home \
    --allow /tmp \
    --allow /etc/pki/ca-trust \
    --allow /etc/pki/tls/certs \
    --allow /home/$USER/.npm \
    --allow "$(pwd)/.git" \
    --allow /path/to/claude-memory \
    --allow /home/$USER/.claude \
    --read /home/$USER/.config/gh \
    --read /home/$USER/.ssh \
    --read-file /etc/group \
    --read-file /etc/passwd \
    --allow-file /etc/machine-id \
    --allow-file /etc/pki/tls/openssl.cnf \
    -- claude
```

Adjust the paths to match your local environment before running.

Run the script from the root of your Keycloak clone.

This sandbox configuration does not allow Claude to manage memory (lack of access). This also helps to make sure the agent is
always starting fresh with no prior knowledge from previous runs, which is good for testing and iterating on the agent configuration. 

NOTE: Still a WIP, but should be enough to get us started with a basic sandbox. We can iterate on this to further restrict access if needed, or loosen it if we find we need more permissions.