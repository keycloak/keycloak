#!/bin/bash
# Displays the count of Dependabot PRs opened in the last 7 days
# and the number of failed Admin UI E2E and Account UI E2E checks.

# Usage: `./gh-dependabot-failure-stats.sh <YOUR GITHUB TOKEN GOES HERE>`

# Sample output:
# Total Open Dependabot PRs: 18
# Total Admin UI E2E failures: 1
# Total Account UI E2E failures: 0

# --- Failure Details for PR #12345 ---
# Title: Bump typescript-eslint from 7.17.0 to 7.18.0
# URL: https://github.com/keycloak/keycloak/pull/12345
# Branch: dependabot/npm_and_yarn/typescript-eslint-7.18.0
# More Info:
#   - Admin UI E2E Check: Admin UI E2E (2, chrome)
#     Details URL: https://github.com/keycloak/keycloak/actions/runs/56789/job/98765
#     Run ID: 56789
#     Run Status: completed
#     Run Conclusion: failure
#     Run HTML URL: https://github.com/keycloak/keycloak/actions/runs/56789
#     Run Attempt: 1

REPO_OWNER="keycloak"
REPO_NAME="keycloak"
total_prs=0
admin_failure_counter=0
account_failure_counter=0
failure_details=""

function get_dependabot_prs_last_7_days() {
  local seven_days_ago
  seven_days_ago=$([ "$(uname)" = Linux ] && date --date="7 days ago" +%Y-%m-%d || date -v -7d +%Y-%m-%d)

  gh pr list --repo "${REPO_OWNER}/${REPO_NAME}" --author "app/dependabot" --state all --json number,createdAt \
    | jq -r --arg seven_days_ago "$seven_days_ago" \
    '.[] | select(.createdAt > $seven_days_ago) | "\(.number)"'
}

function get_pr_details() {
  local pr_number=$1
  gh pr view "$pr_number" --repo "${REPO_OWNER}/${REPO_NAME}" --json number,title,headRefName,url
}

function get_failed_runs() {
  local pr_number=$1
  local commit_sha
  commit_sha=$(gh pr view "$pr_number" --repo "${REPO_OWNER}/${REPO_NAME}" --json commits -q '.commits[-1].oid')

  gh api -X GET "/repos/${REPO_OWNER}/${REPO_NAME}/commits/${commit_sha}/check-runs" \
    --jq '.check_runs[] | select(.conclusion == "failure")'
}

function get_run_details() {
  local run_id=$1
  gh api -X GET "/repos/${REPO_OWNER}/${REPO_NAME}/actions/runs/${run_id}" \
    --jq '{run_id: .id, status: .status, conclusion: .conclusion, html_url: .html_url, run_attempt: .run_attempt, jobs_url: .jobs_url}'
}

function get_failed_jobs_details() {
  local jobs_url=$1
  gh api -X GET "$jobs_url" \
    --jq '.jobs[] | select(.conclusion == "failure" and (.name | contains("Admin UI E2E") or contains("Account UI E2E")))'
}

function get_failed_dependabot_prs_details() {
  local dependabot_prs_last_7_days
  dependabot_prs_last_7_days=$(get_dependabot_prs_last_7_days)
  total_prs=$(echo "$dependabot_prs_last_7_days" | wc -l)
  admin_failure_counter=0
  account_failure_counter=0
  failure_details=""

  while read -r pr_number; do
    local pr_details
    local title
    local url
    local head_ref
    local failed_runs
    local relevant_failures=""
    local admin_failure_added=0
    local account_failure_added=0
    
    pr_details=$(get_pr_details "$pr_number")
    title=$(echo "$pr_details" | jq -r '.title')
    url=$(echo "$pr_details" | jq -r '.url')
    head_ref=$(echo "$pr_details" | jq -r '.headRefName')
    failed_runs=$(get_failed_runs "$pr_number")

    if [ -n "$failed_runs" ]; then
      while read -r check; do
        local check_name
        local details_url
        local run_id
        local run_details
        local run_status
        local run_conclusion
        local run_html_url
        local run_attempt
        local jobs_url
        local failed_jobs

        check_name=$(echo "$check" | jq -r '.name')
        details_url=$(echo "$check" | jq -r '.details_url')
        run_id=$(echo "$details_url" | sed -E 's#.*/actions/runs/([0-9]+)/.*#\1#')
        run_details=$(get_run_details "$run_id")
        run_status=$(echo "$run_details" | jq -r '.status')
        run_conclusion=$(echo "$run_details" | jq -r '.conclusion')
        run_html_url=$(echo "$run_details" | jq -r '.html_url')
        run_attempt=$(echo "$run_details" | jq -r '.run_attempt')
        jobs_url=$(echo "$run_details" | jq -r '.jobs_url')
        failed_jobs=$(get_failed_jobs_details "$jobs_url")

        if [ -n "$failed_jobs" ]; then
          while read -r job; do
            local job_name
            local job_url

            job_name=$(echo "$job" | jq -r '.name')
            job_url=$(echo "$job" | jq -r '.html_url')

            if [[ "$job_name" == *"Admin UI E2E"* && "$admin_failure_added" -eq 0 ]]; then
              relevant_failures+="  - Admin UI E2E Check: $job_name\n"
              relevant_failures+="    Details URL: $job_url\n"
              relevant_failures+="    Run ID: $run_id\n"
              relevant_failures+="    Run Status: $run_status\n"
              relevant_failures+="    Run Conclusion: $run_conclusion\n"
              relevant_failures+="    Run HTML URL: $run_html_url\n"
              relevant_failures+="    Run Attempt: $run_attempt\n"
              relevant_failures+="\n"
              admin_failure_counter=$((admin_failure_counter + 1))
              admin_failure_added=1
            fi

            if [[ "$job_name" == *"Account UI E2E"* && "$account_failure_added" -eq 0 ]]; then
              relevant_failures+="  - Account UI E2E Check: $job_name\n"
              relevant_failures+="    Details URL: $job_url\n"
              relevant_failures+="    Run ID: $run_id\n"
              relevant_failures+="    Run Status: $run_status\n"
              relevant_failures+="    Run Conclusion: $run_conclusion\n"
              relevant_failures+="    Run HTML URL: $run_html_url\n"
              relevant_failures+="    Run Attempt: $run_attempt\n"
              relevant_failures+="\n"
              account_failure_counter=$((account_failure_counter + 1))
              account_failure_added=1
            fi
          done <<< "$(echo "$failed_jobs")"
        fi
      done <<< "$(echo "$failed_runs")"

      if [ -n "$relevant_failures" ]; then
        failure_details+="--- Failure Details for PR #$pr_number ---\n"
        failure_details+="Title: $title\n"
        failure_details+="URL: $url\n"
        failure_details+="Branch: $head_ref\n"
        failure_details+="More Info:\n"
        failure_details+="$relevant_failures"
      fi
    fi
  done <<< "$dependabot_prs_last_7_days"
}

function get_stats() {
  echo "Total Opened Dependabot PRs: $total_prs"
  echo "Total Admin UI E2E failures: $admin_failure_counter"
  echo "Total Account UI E2E failures: $account_failure_counter"
  echo ""
  echo -e "$failure_details"
}

get_failed_dependabot_prs_details
get_stats