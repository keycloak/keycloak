#!/usr/bin/env bash

MODULES=$(readlink -f "$1")
LICENSES=$(readlink -f "$2")

for i in $(find "$MODULES" -name '*.jar'); do
	if (unzip -l "$i" | grep -q pom.properties); then
		props=$(unzip -p "$i" '*/pom.properties')
		groupId=$(printf '%s\n'    "$props" | grep '^groupId='    | head -1 | cut -d= -f2- | tr -d '[:space:]')
		artifactId=$(printf '%s\n' "$props" | grep '^artifactId=' | head -1 | cut -d= -f2- | tr -d '[:space:]')
		version=$(printf '%s\n'    "$props" | grep '^version='    | head -1 | cut -d= -f2- | tr -d '[:space:]')

		if [ ! -f "$LICENSES/$groupId,$artifactId,$version,"* ]; then
			echo "Missing: "
			echo "group: $groupId"
			echo "artifact: $artifactId"
			echo "version: $version"
			echo "---------------------"
		fi
	else
		echo "Not Maven: $i"
		echo "---------------------"
	fi
done
