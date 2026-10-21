#!/bin/bash -e

function help() {
     echo "Download logs for failed GitHub Actions runs"
     echo
     echo "options:"
     echo "-b     Branch to use (defaults to main)"
     echo "-w     Workflow name (required)"
     echo "-d     Date range (defaults to today)"
     echo "-l     Download directory (defaults to logs)"
     echo
}

while getopts ":b:d:w:l:" option; do
  case $option in
    b)
      BRANCH=$OPTARG;;
    w)
      WORKFLOW=$OPTARG;;
    d)
      DATE=$OPTARG;;
    l)
      LOGS=$OPTARG;;
    *)
      help
      exit;;
  esac
done

if [ "$DATE" == "" ]; then
  DATE=$(date -Idate)
fi

USER=$(gh api user | jq -r '.login')

if [ "$BRANCH" == "" ]; then
  BRANCH="main"
fi

if [ "$WORKFLOW" == "" ]; then
  echo -e "Error: Workflow not specified\n" && help && exit 1
  exit 1
fi

if [ "$LOGS" == "" ]; then
  LOGS="logs"
fi

if [ ! -d "$LOGS" ]; then
    mkdir "$LOGS"
fi

for i in $(gh run list -L 100 -R "$USER/keycloak" -w "$WORKFLOW" -s failure --created "$DATE" --json databaseId | jq -r .[].databaseId); do
    echo -n "($i) "
    if [ ! -f "$LOGS/$i" ]; then
        gh run -R "$USER/keycloak" view --log-failed "$i" > "$LOGS/$i"
    fi
    if [ ! -f "$LOGS/$i.json" ]; then
        gh run -R "$USER/keycloak" view --json name,conclusion,databaseId,jobs "$i" > "$LOGS/$i.json"
    fi
done
