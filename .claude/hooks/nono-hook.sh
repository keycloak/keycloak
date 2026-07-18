#!/bin/bash
# nono-hook.sh - Claude Code hook for nono sandbox diagnostics
# Version: 0.0.1
#
# This hook is automatically installed by nono when using the claude-code profile.
# It injects sandbox capability information when tool operations fail.

# Only run if we're inside a nono sandbox
if [ -z "$NONO_CAP_FILE" ] || [ ! -f "$NONO_CAP_FILE" ]; then
    exit 0
fi

# Check if jq is available (required for JSON parsing)
if ! command -v jq &> /dev/null; then
    exit 0
fi

# Read capabilities from the cap file
CAPS=$(jq -r '.fs[] | "  " + (.resolved // .path) + " (" + .access + ")"' "$NONO_CAP_FILE" 2>/dev/null)
NET=$(jq -r 'if .net_blocked then "blocked" else "allowed" end' "$NONO_CAP_FILE" 2>/dev/null)

# Build context message with clear, actionable instructions
CONTEXT="[NONO SANDBOX - PERMISSION DENIED]

STOP. Do not try alternative approaches. This is a hard security boundary.

You are running inside the nono security sandbox. The operation you just attempted is PERMANENTLY BLOCKED for this session. There is NO workaround possible from inside this session.

ALLOWED PATHS (everything else is blocked):
$CAPS
Network: $NET

FORBIDDEN ACTIONS - DO NOT ATTEMPT:
- Trying alternative file paths
- Copying files to allowed locations
- Using sudo or changing permissions
- Suggesting the user run commands manually
- Any other workaround

REQUIRED ACTION:
Tell the user they must EXIT this Claude session and restart with the needed path:

  nono run --allow /path/to/needed -- claude

This is the ONLY solution. The sandbox cannot be modified from within."

# Output JSON for Claude Code hook system
jq -n --arg ctx "$CONTEXT" '{
  "hookSpecificOutput": {
    "hookEventName": "PostToolUseFailure",
    "additionalContext": $ctx
  }
}'
