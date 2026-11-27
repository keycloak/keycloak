#!/usr/bin/env bash
set -euo pipefail

DRY_RUN="${DRY_RUN:-false}"
ISSUE_NUMBER="${ISSUE_NUMBER:-}"
ACTION="${ACTION:-closed}"
REPO="${REPO:-${GITHUB_REPOSITORY:-keycloak/keycloak}}"

ACTION_SET=false
REPO_SET=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run|-n)
      DRY_RUN=true
      shift
      ;;
    -h|--help)
      echo "Usage: $0 [--dry-run] [ISSUE_NUMBER] [ACTION] [REPO]"
      echo "Handles open PRs linked to an issue when that issue is closed or reopened."
      echo "ACTION can be 'closed' (default) or 'reopened'."
      exit 0
      ;;
    *)
      if [ -z "$ISSUE_NUMBER" ]; then
        ISSUE_NUMBER="$1"
      elif [ "$ACTION_SET" = false ]; then
        ACTION="$1"
        ACTION_SET=true
      elif [ "$REPO_SET" = false ]; then
        REPO="$1"
        REPO_SET=true
      else
        echo "Unexpected argument: $1" >&2
        echo "Usage: $0 [--dry-run] [ISSUE_NUMBER] [ACTION] [REPO]" >&2
        exit 1
      fi
      shift
      ;;
  esac
done

if [ "$DRY_RUN" = "1" ] || [ "$DRY_RUN" = "true" ]; then
  DRY_RUN=true
else
  DRY_RUN=false
fi

if [ -z "$ISSUE_NUMBER" ]; then
  echo "Error: ISSUE_NUMBER is required" >&2
  echo "Usage: $0 [--dry-run] [ISSUE_NUMBER] [ACTION] [REPO]" >&2
  exit 1
fi

SKIP_LABEL="skip-issue-check"
EXPIRED_LABEL="status/expired-by-bot"
ISSUE_EXPIRED_PR_LABEL="status/issue-expired"

REPO_OWNER="${REPO%%/*}"
REPO_NAME="${REPO##*/}"

# Find open PRs that currently close this issue.
#
# timelineItems (CrossReferencedEvent) is NOT used here because the GitHub timeline is
# append-only: events are never removed when a PR description is edited to drop a reference.
# Instead we use a two-step approach that reflects live state:
#   1. Search API: find open PRs in this repo that mention the issue number (fast, may
#      over-fetch PRs that mention it without a closing keyword).
#   2. For each candidate, verify via closingIssuesReferences on the PR — this field is
#      re-derived from the current PR description on every request and is therefore
#      authoritative about whether the PR will actually close the issue when merged.

# Step 1: candidate PRs from search.
if ! CANDIDATE_PRS=$(
  gh api graphql \
    -F searchQuery="repo:${REPO} is:pr is:open #${ISSUE_NUMBER}" \
    -f query='
      query($searchQuery: String!) {
        search(query: $searchQuery, type: ISSUE, first: 50) {
          nodes {
            ... on PullRequest {
              number
              labels(first: 20) {
                nodes { name }
              }
            }
          }
        }
      }
    ' 2>&1
); then
  echo "::error::Failed to search for linked pull requests for issue #$ISSUE_NUMBER: $CANDIDATE_PRS"
  exit 1
fi

mapfile -t CANDIDATE_NUMBERS < <(
  printf '%s\n' "$CANDIDATE_PRS" | jq -r '
    .data.search.nodes[] | select(.number != null) | .number
  ' 2>/dev/null | sort -u || true
)

# Step 2: confirm each candidate still closes this issue via closingIssuesReferences.
PR_NUMBERS=()
for CANDIDATE in "${CANDIDATE_NUMBERS[@]}"; do
  [ -z "$CANDIDATE" ] && continue
  if ! CLOSING_DATA=$(
    gh api graphql \
      -F owner="$REPO_OWNER" \
      -F repo="$REPO_NAME" \
      -F prNumber="$CANDIDATE" \
      -f query='
        query($owner: String!, $repo: String!, $prNumber: Int!) {
          repository(owner: $owner, name: $repo) {
            pullRequest(number: $prNumber) {
              closingIssuesReferences(first: 50) {
                nodes { number }
              }
            }
          }
        }
      ' 2>&1
  ); then
    echo "::warning::Failed to verify closingIssuesReferences for PR #$CANDIDATE: $CLOSING_DATA — skipping."
    continue
  fi
  if printf '%s\n' "$CLOSING_DATA" | jq -e \
      --argjson n "$ISSUE_NUMBER" \
      '.data.repository.pullRequest.closingIssuesReferences.nodes[] | select(.number == $n)' \
      > /dev/null 2>&1; then
    PR_NUMBERS+=("$CANDIDATE")
  fi
done

if [ "${#PR_NUMBERS[@]}" -eq 0 ]; then
  echo "No open pull requests currently closing issue #$ISSUE_NUMBER."
  exit 0
fi

if [ "$ACTION" = "reopened" ]; then
  # On reopen: remove status/issue-expired from every linked open PR.
  for PR_NUMBER in "${PR_NUMBERS[@]}"; do
    PR_LABELS=$(
      printf '%s\n' "$CANDIDATE_PRS" | jq -r --argjson n "$PR_NUMBER" '
        .data.search.nodes[] | select(.number == $n) | .labels.nodes[].name
      ' 2>/dev/null || true
    )
    if printf '%s\n' "$PR_LABELS" | grep -Fxq -- "$ISSUE_EXPIRED_PR_LABEL"; then
      if [ "$DRY_RUN" = "true" ]; then
        echo "[DRY-RUN] Issue #$ISSUE_NUMBER reopened — would remove '$ISSUE_EXPIRED_PR_LABEL' from PR #$PR_NUMBER."
      else
        echo "Issue #$ISSUE_NUMBER reopened — removing '$ISSUE_EXPIRED_PR_LABEL' from PR #$PR_NUMBER."
        gh pr edit "$PR_NUMBER" -R "$REPO" --remove-label "$ISSUE_EXPIRED_PR_LABEL"
      fi
    else
      echo "PR #$PR_NUMBER does not have '$ISSUE_EXPIRED_PR_LABEL' — nothing to remove."
    fi
  done
  exit 0
fi

# On close: check whether the issue was expired by the bot.
ISSUE_LABELS=$(gh api "repos/$REPO/issues/$ISSUE_NUMBER/labels" --jq '.[].name' || true)
IS_EXPIRED_BY_BOT=0
if printf '%s\n' "$ISSUE_LABELS" | grep -Fxq -- "$EXPIRED_LABEL"; then
  IS_EXPIRED_BY_BOT=1
fi

for PR_NUMBER in "${PR_NUMBERS[@]}"; do
  # Check whether the PR carries the skip-issue-check label.
  PR_LABELS=$(
    printf '%s\n' "$CANDIDATE_PRS" | jq -r --argjson n "$PR_NUMBER" '
      .data.search.nodes[] | select(.number == $n) | .labels.nodes[].name
    ' 2>/dev/null || true
  )

  if printf '%s\n' "$PR_LABELS" | grep -Fxq -- "$SKIP_LABEL"; then
    echo "PR #$PR_NUMBER has '$SKIP_LABEL' — skipping."
    continue
  fi

  if [ "$IS_EXPIRED_BY_BOT" -eq 1 ]; then
    if [ "$DRY_RUN" = "true" ]; then
      echo "[DRY-RUN] Issue #$ISSUE_NUMBER was expired by bot — would add '$ISSUE_EXPIRED_PR_LABEL' to PR #$PR_NUMBER."
    else
      echo "Issue #$ISSUE_NUMBER was expired by bot — adding '$ISSUE_EXPIRED_PR_LABEL' to PR #$PR_NUMBER."
      gh pr edit "$PR_NUMBER" -R "$REPO" --add-label "$ISSUE_EXPIRED_PR_LABEL"
    fi
  else
    if [ "$DRY_RUN" = "true" ]; then
      echo "[DRY-RUN] Would close PR #$PR_NUMBER because linked issue #$ISSUE_NUMBER was closed."
    else
      echo "Closing PR #$PR_NUMBER because linked issue #$ISSUE_NUMBER has been closed."
      gh pr close "$PR_NUMBER" -R "$REPO" \
        --comment "Closing this pull request because the linked issue #$ISSUE_NUMBER has been closed."
    fi
  fi
done
