#!/usr/bin/env bash
set -e

TEAMS="teams.yml"
BUG="ISSUE_TEMPLATE/bug.yml"

GH_AREAS=( $(gh label list --search 'area/' --limit 100 --json name | jq -r .[].name | sort) )
LOCAL_AREAS=( $(cat teams.yml | grep 'area/' | sed 's/.*- //') )
BUG_TEMPLATE_AREAS=( $(yq '.body.[] | select(.id | test("area")) | .attributes.options.[]' ISSUE_TEMPLATE/bug.yml | grep -v '^$') )

TEAMS_VALID=true
BUG_VALID=true

echo "Checking: $TEAMS"
for AREA in "${GH_AREAS[@]}"; do
    if ! grep -q -F -w "$AREA" <<<"${LOCAL_AREAS[@]}"; then
        echo "[$AREA] missing in $TEAMS"
        TEAMS_VALID=false
    fi
done

for AREA in "${LOCAL_AREAS[@]}"; do
    if ! grep -q -F -w "$AREA" <<<"${GH_AREAS[@]}"; then
        echo "[$AREA] missing in GitHub"
        TEAMS_VALID=false
    fi
done

if [ "$TEAMS_VALID" = true ]; then
    echo "[OK]"
fi

echo ""

echo "Checking: $BUG"
for AREA in "${GH_AREAS[@]}"; do
    AREA_SHORT=$(echo $AREA | sed 's|area/||g')
    if ! grep -q -F -w "$AREA_SHORT" <<<"${BUG_TEMPLATE_AREAS[@]}"; then
        echo "[$AREA] missing in $BUG"
        BUG_VALID=false
    fi
done

for AREA in "${BUG_TEMPLATE_AREAS[@]}"; do
    AREA_LONG="area/$AREA"
    if ! grep -q -F -w "$AREA_LONG" <<<"${GH_AREAS[@]}"; then
        echo "[$AREA_LONG] missing in GitHub"
        BUG_VALID=false
    fi
done

if [ "$BUG_VALID" = true ]; then
    echo "[OK]"
fi

if [ "$TEAMS_VALID" != true ] || [ "$BUG_VALID" != true ]; then
    exit 1
fi
