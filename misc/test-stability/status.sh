#!/bin/bash -e

function help() {
     echo "View status of GitHub Actions runs"
     echo
     echo "options:"
     echo "-b     Branch to use (defaults to main)"
     echo "-w     Workflow name (required)"
     echo "-d     Date range (defaults to today)"
     echo
}

while getopts ":b:d:w:" option; do
  case $option in
    b)
      BRANCH=$OPTARG;;
    w)
      WORKFLOW=$OPTARG;;
    d)
      DATE=$OPTARG;;
    *)
      help
      exit;;
  esac
done

if [ "$DATE" == "" ]; then
  DATE=$(date -Idate)
fi

if [ "$BRANCH" == "" ]; then
  BRANCH="main"
fi

if [ "$WORKFLOW" == "" ]; then
  echo -e "Error: Workflow not specified\n" && help && exit 1
fi

USER=$(gh api user | jq -r '.login')

echo "Status of $WORKFLOW in $USER/keycloak for branch $BRANCH"
echo ""

gh api -X GET "/repos/$USER/keycloak/actions/workflows/$WORKFLOW/runs" -F branch="$BRANCH" -F per_page=100 --paginate -F created="$DATE" | jq -r '.workflow_runs[] | [.status, .conclusion] | @csv' | sort | uniq -c
