#!/bin/bash -e

function help() {
     echo "Schedule many runs for a GitHub Actions workflow"
     echo
     echo "options:"
     echo "-b     Branch to use (defaults to main)"
     echo "-w     Workflow name (required)"
     echo "-n     Number of runs (defaults to 100)"
     echo
}

while getopts ":b:n:w:" option; do
  case $option in
    b)
      BRANCH=$OPTARG;;
    w)
      WORKFLOW=$OPTARG;;
    n)
      RUNS=$OPTARG;;
    *)
      help
      exit;;
  esac
done

if [ "$RUNS" == "" ]; then
  RUNS=100
fi

if [ "$WORKFLOW" == "" ]; then
  echo -e "Error: Workflow not specified\n" && help && exit 1
fi

if [ "$BRANCH" == "" ]; then
  BRANCH="main"
fi

USER=$(gh api user | jq -r '.login')

echo "Scheduling $RUNS run(s) in $USER/keycloak for workflow $WORKFLOW and branch $BRANCH"
echo ""

for i in $(seq 1 "$RUNS"); do
    echo -n "($i) "
    gh workflow run -R "$USER/keycloak" -r "$BRANCH" "$WORKFLOW"
done
