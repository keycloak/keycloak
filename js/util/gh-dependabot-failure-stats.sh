#!/bin/bash
# Displays the count of Dependabot PRs opened in the last 7 days
# and the number of failed Admin UI E2E and Account UI E2E checks.

# Usage: `./gh-dependabot-failure-stats.sh <YOUR GITHUB TOKEN GOES HERE>`

# Sample output:
# Total Open Dependabot PRs: 18
# Total Admin UI E2E failures: 1
# Total Account UI E2E failures: 0

# --- Failure Details for PR #x ---
# Title: Bump typescript-eslint from 7.17.0 to 7.18.0
# URL: https://github.com/keycloak/keycloak/pull/x
# Branch: dependabot/npm_and_yarn/typescript-eslint-7.18.0
# More Info:
#   - Admin UI E2E Check: Admin UI E2E (2, chrome)
#     Details URL: https://github.com/keycloak/keycloak/actions/runs/y/job/z
#     Run ID: y
#     Run Status: completed
#     Run Conclusion: failure
#     Run HTML URL: https://github.com/keycloak/keycloak/actions/runs/y
#     Run Attempt: 1

REPO_OWNER="keycloak"
REPO_NAME="keycloak"
total_prs=0
admin_failure_counter=0
account_failure_counter=0
failure_details=""

function fetch_open_dependabot_prs() {
  local seven_days_ago
  local today
 
  seven_days_ago=$(date -v -7d +%Y-%m-%d)
  today=$(date +%Y-%m-%d)

  gh pr list --repo "${REPO_OWNER}/${REPO_NAME}" --author "app/dependabot" --state open --json number,createdAt | 
  jq -r --arg seven_days_ago "$seven_days_ago" \
    '.[] | select(.createdAt > $seven_days_ago) | "\(.number)"'

  gh pr list --repo "${REPO_OWNER}/${REPO_NAME}" --author "app/dependabot" --state closed --json number,createdAt | 
  jq -r --arg seven_days_ago "$seven_days_ago" \
    '.[] | select(.createdAt > $seven_days_ago) | "\(.number)"'
}

function get_pr_details() {
  local pr_number=$1
  gh pr view "$pr_number" --repo "${REPO_OWNER}/${REPO_NAME}" --json number,title,headRefName,url
}

function check_failure_status() {
  local pr_number=$1
  gh api -X GET "/repos/${REPO_OWNER}/${REPO_NAME}/commits/$(gh pr view "$pr_number" --repo "${REPO_OWNER}/${REPO_NAME}" --json headRefOid -q .headRefOid)/check-runs" -q '.check_runs[] | select(.conclusion == "failure")'
}

function get_run_details() {
  local run_id=$1
  gh api -X GET "/repos/${REPO_OWNER}/${REPO_NAME}/actions/runs/${run_id}" --jq '{run_id: .id, status: .status, conclusion: .conclusion, html_url: .html_url, run_attempt: .run_attempt}'
}

function get_failed_dependabot_prs_details() {
  local open_dependabot_prs
  open_dependabot_prs=$(fetch_open_dependabot_prs)

  total_prs=$(echo "$open_dependabot_prs" | wc -l)
  admin_failure_counter=0
  account_failure_counter=0
  failure_details=""

  while read -r pr_number; do
    local pr_details
    pr_details=$(get_pr_details "$pr_number")

    local title
    local url
    local head_ref
    title=$(echo "$pr_details" | jq -r '.title')
    url=$(echo "$pr_details" | jq -r '.url')
    head_ref=$(echo "$pr_details" | jq -r '.headRefName')

    local failure_checks
    failure_checks=$(check_failure_status "$pr_number")

    if [ -n "$failure_checks" ]; then
      failure_details+="--- Failure Details for PR #$pr_number ---\n"
      failure_details+="Title: $title\n"
      failure_details+="URL: $url\n"
      failure_details+="Branch: $head_ref\n"
      failure_details+="More Info:\n"
      while read -r check; do
        local check_name
        local details_url
        local run_id
        check_name=$(echo "$check" | jq -r '.name')
        details_url=$(echo "$check" | jq -r '.details_url')
        run_id=$(echo "$details_url" | sed -E 's#.*/actions/runs/([0-9]+)/.*#\1#')

        local run_details
        local run_status
        local run_conclusion
        local run_html_url
        local run_attempt
        run_details=$(get_run_details "$run_id")
        run_status=$(echo "$run_details" | jq -r '.status')
        run_conclusion=$(echo "$run_details" | jq -r '.conclusion')
        run_html_url=$(echo "$run_details" | jq -r '.html_url')
        run_attempt=$(echo "$run_details" | jq -r '.run_attempt')

        if [[ "$check_name" == *"Admin UI E2E"* ]]; then
          failure_details+="  - Admin UI E2E Check: $check_name\n"
          failure_details+="    Details URL: $details_url\n"
          failure_details+="    Run ID: $run_id\n"
          failure_details+="    Run Status: $run_status\n"
          failure_details+="    Run Conclusion: $run_conclusion\n"
          failure_details+="    Run HTML URL: $run_html_url\n"
          failure_details+="    Run Attempt: $run_attempt\n"
          failure_details+="\n"
          admin_failure_counter=$((admin_failure_counter + 1))
        fi

        if [[ "$check_name" == *"Account UI E2E"* ]]; then
          failure_details+="  - Account UI E2E Check: $check_name\n"
          failure_details+="    Details URL: $details_url\n"
          failure_details+="    Run ID: $run_id\n"
          failure_details+="    Run Status: $run_status\n"
          failure_details+="    Run Conclusion: $run_conclusion\n"
          failure_details+="    Run HTML URL: $run_html_url\n"
          failure_details+="    Run Attempt: $run_attempt\n"
          failure_details+="\n"
          account_failure_counter=$((account_failure_counter + 1))
        fi
      done <<< "$(echo "$failure_checks" | jq -c '.')"
    fi
  done <<< "$open_dependabot_prs"
}

function display_stats() {
  echo "Total Open Dependabot PRs: $total_prs"
  echo "Total Admin UI E2E failures: $admin_failure_counter"
  echo "Total Account UI E2E failures: $account_failure_counter"
  echo ""
  echo -e "$failure_details"
}

get_failed_dependabot_prs_details
display_stats