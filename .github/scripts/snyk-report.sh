#!/bin/bash -e

KEYCLOAK_REPO="keycloak/keycloak"
BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD)

# Extract the version number if BRANCH_NAME contains a slash
if [[ $BRANCH_NAME == *\/* ]]; then
    BRANCH_NAME=$(echo $BRANCH_NAME | grep -oP '\d+\.\d+')
fi

# Prevent duplicates by checking if a similar CVE ID exists
check_github_issue_exists() {
    local issue_title="$1"
    # Extract the CVE ID
    local CVE_ID=$(echo "$issue_title" | grep -oE '(CVE-[0-9]{4}-[0-9]{4,7}|SNYK-[A-Z]+-[A-Z0-9]+-[0-9]{4,7})')
    local search_url="https://api.github.com/search/issues?q=$CVE_ID+is%3Aissue+sort%3Aupdated-desc+repo:$KEYCLOAK_REPO"
    local response=$(curl -f -s -H "Authorization: token $GITHUB_TOKEN" -H "Accept: application/vnd.github.v3+json" "$search_url")
    local count=$(echo "$response" | jq '.total_count')

    # Check for bad credentials
    if printf "%s" "$response" | jq -e '.message == "Bad credentials"' > /dev/null; then
        printf "Error: Bad credentials\n%s\n" "$response"
        echo "Error: Bad credentials. Aborting script."
        exit 1
    fi

    # Check for rate limiting
    if printf "%s" "$response" | jq -e '.message == "API rate limit exceeded"' > /dev/null; then
        printf "Error: API rate limit exceeded\n%s\n" "$response"
        exit 1
    fi

    # Check if total_count is available
    if [[ $count == "null" ]]; then
        printf "Error: total_count not available in response\n%s\n" "$response"
        exit 1
    fi

    if [[ $count -gt 0 ]]; then
        local issue_id=$(echo "$response" | jq -r '.items[0].number')
        echo "$issue_id"
    else
        echo "1"
    fi
}

# Create a GH issue based on the content of the CVE
create_github_issue() {
    local title="$1"
    local body="$2"

    local api_url="https://api.github.com/repos/$KEYCLOAK_REPO/issues"
    local data=$(jq -n --arg title "$title" --arg body "$body" --arg branch "backport/$BRANCH_NAME" \
                 '{title: $title, body: $body, labels: ["status/triage", "kind/cve", "kind/bug", $branch]}')
    local response=$(curl -f -s -w "%{http_code}" -X POST -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/json" -d "$data" "$api_url")
    local http_code=$(echo "$response" | tail -n1)

    if [[ $http_code -eq 201 ]]; then
        return 0
    else
        printf "Issue creation failed with status: %s\n" "$http_code"
        exit 1
    fi
}

# Update existing issue based on the branches affected
update_github_issue() {
    local issue_id="$1"
    local api_url="https://api.github.com/repos/$KEYCLOAK_REPO/issues/$issue_id"
    local existing_labels=$(curl -f -s -H "Authorization: token $GITHUB_TOKEN" -H "Accept: application/vnd.github.v3+json" "$api_url" | jq '.labels | .[].name' | jq -s .)
    local new_label="backport/$BRANCH_NAME"
    local updated_labels=$(echo "$existing_labels" | jq --arg new_label "$new_label" '. + [$new_label] | unique')
    local data=$(jq -n --argjson labels "$updated_labels" '{labels: $labels}')
    local response=$(curl -f -s -w "%{http_code}" -X PATCH -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/json" -d "$data" "$api_url")
    local http_code=$(echo "$response" | tail -n1)

    if [[ $http_code -eq 200 ]]; then
        return 0
    else
        printf "Issue update failed with status: %s\n" "$http_code"
        exit 1
    fi
}

check_dependencies() {
    command -v jq >/dev/null 2>&1 || { echo >&2 "jq is required. Exiting."; exit 1; }
}

# Parse the CVE report coming from SNYK
parse_and_process_vulnerabilities() {
    jq -c '.vulnerabilities[] | select(.type != "license")' | while IFS= read -r vulnerability; do
        local cve_title=$(echo "$vulnerability" | jq -r '(.identifiers.CVE[0] // .id) + " - " + (.title // "N/A")')
        local module=$(echo "$vulnerability" | jq -r '((.mavenModuleName.groupId // "unknown") + ":" + (.mavenModuleName.artifactId // "unknown"))')
        local title="${cve_title} in ${module}"
        local from_path=$(echo "$vulnerability" | jq -r 'if .from != [] then "Introduced through: " + (.from | join(" › ")) else "" end')
        local description=$(echo "$vulnerability" | jq -r '.description // "N/A"')

        printf -v body "%s\n%s\n%s\n%s" "$title" "$module" "$from_path" "$description"
        issue_id=$(check_github_issue_exists "$cve_title")
        if [[ $issue_id -eq 1 ]]; then
            create_github_issue "$title" "$body"
        else
            update_github_issue "$issue_id"
        fi
    done
}

main() {
    check_dependencies

    if [ -t 0 ]; then
        echo "Error: No input provided. Please pipe in a JSON file."
        echo "Usage: cat snyk-report.json | $0"
        exit 1
    else
        parse_and_process_vulnerabilities
    fi
}

main "$@"
