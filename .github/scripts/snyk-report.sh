#!/bin/bash -e

KEYCLOAK_REPO="keycloak/keycloak"

# Prevent duplicates by checking if a similar title exists
check_github_issue_exists() {
    local issue_title="$1"
    # Extract the CVE ID
    local CVE_ID=$(echo "$issue_title" | grep -oE '(CVE-[0-9]{4}-[0-9]{4,7}|SNYK-[A-Z]+-[A-Z0-9]+-[0-9]{4,7})')
    local search_url="https://api.github.com/search/issues?q=$CVE_ID+is%3Aissue+sort%3Aupdated-desc+repo:$KEYCLOAK_REPO"
    local response=$(curl -s -H "Authorization: token $GITHUB_TOKEN" -H "Accept: application/vnd.github.v3+json" "$search_url")
    local count=$(echo "$response" | jq '.total_count')
    
    if [[ $count -gt 0 ]]; then
        return 0  
    else
        return 1 
    fi
}

# Create a GH issue based on the content of the CVE
create_github_issue() {
    local title="$1"
    local body="$2"

    local api_url="https://api.github.com/repos/$KEYCLOAK_REPO/issues"
    local data=$(jq -n --arg title "$title" --arg body "$body" \
                 '{title: $title, body: $body, labels: ["status/triage", "kind/cve", "kind/bug"]}')
    local response=$(curl -s -w "%{http_code}" -X POST -H "Authorization: token $GITHUB_TOKEN" -H "Content-Type: application/json" -d "$data" "$api_url")
}

check_dependencies() {
    command -v jq >/dev/null 2>&1 || { echo >&2 "jq is required. Exiting."; exit 1; }
}

# Parse the CVE report coming from SNYK
parse_and_print_vulnerabilities() {
    jq -c '.vulnerabilities[] | select(.type != "license")' | while IFS= read -r vulnerability; do
        local cve_title=$(echo "$vulnerability" | jq -r '(.identifiers.CVE[0] // .id) + " - " + (.title // "N/A")')
        local module=$(echo "$vulnerability" | jq -r '((.mavenModuleName.groupId // "unknown") + ":" + (.mavenModuleName.artifactId // "unknown"))')
        local title="${cve_title} in ${module}"
        local from_path=$(echo "$vulnerability" | jq -r 'if .from != [] then "Introduced through: " + (.from | join(" › ")) else "" end')
        local description=$(echo "$vulnerability" | jq -r '.description // "N/A"')

        printf -v body "%s\n%s\n%s\n%s" "$title" "$module" "$from_path" "$description"
        if ! check_github_issue_exists "$cve_title"; then
            create_github_issue "$title" "$body"
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
        parse_and_print_vulnerabilities
    fi
}

main "$@"
