#!/bin/bash

# Tool to find out where a dependency version is declared. Dependency versions can come from an imported BOM,
# keycloak-parent, a child module, and there may even be multiple different versions declared.
#
# Uses mvnw to generate an effective-pom file that is cached in the tmp directory allowing faster subsequent lookups
#
# Examples:
# $misc/scripts/find-dependency.sh org.bouncycastle:bcutil-jdk18on
# $misc/scripts/find-dependency.sh org.seleniumhq.selenium:selenium-server
# $misc/scripts/find-dependency.sh org.apache.mina:mina-core

set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Usage: $0 <groupId:artifactId>"
    exit 1
fi

GROUP_ID="${1%%:*}"
ARTIFACT_ID="${1##*:}"

if [ -z "$GROUP_ID" ] || [ -z "$ARTIFACT_ID" ] || [ "$GROUP_ID" = "$ARTIFACT_ID" ]; then
    echo "Error: argument must be in the format groupId:artifactId"
    exit 1
fi

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
POM_FILE="/tmp/kc-effective-pom.xml"
HASH_FILE="/tmp/kc-effective-pom.hash"
GAV_MAP_FILE="/tmp/kc-gav-map.txt"

current_hash=$(find "$PROJECT_ROOT" -name pom.xml -not -path '*/target/*' | sort | xargs cat | sha256sum | awk '{print $1}')

if [ -f "$POM_FILE" ] && [ -f "$HASH_FILE" ] && [ -f "$GAV_MAP_FILE" ] && [ "$(cat "$HASH_FILE")" = "$current_hash" ]; then
    echo "Using cached effective POM"
else
    echo "Generating effective POM (this may take a while)..."
    "$PROJECT_ROOT/mvnw" -f "$PROJECT_ROOT/pom.xml" help:effective-pom -Dverbose -Doutput="$POM_FILE" -q 2>/dev/null

    echo "Building module mapping..."
    > "$GAV_MAP_FILE"
    while IFS= read -r -d '' pom; do
        rel_path="${pom#$PROJECT_ROOT/}"
        gav=$(awk '
            /<parent>/ { in_parent=1 }
            /<\/parent>/ { in_parent=0 }
            !in_parent && (/<dependencies>/ || /<dependencyManagement>/ || /<build>/ || /<profiles>/) { stop=1 }
            !in_parent && !stop && /<groupId>/ && !gid { match($0, /<groupId>([^<]+)<\/groupId>/, g); gid=g[1] }
            !in_parent && !stop && /<artifactId>/ && !aid { match($0, /<artifactId>([^<]+)<\/artifactId>/, a); aid=a[1] }
            in_parent && /<groupId>/ && !pgid { match($0, /<groupId>([^<]+)<\/groupId>/, pg); pgid=pg[1] }
            END {
                if (!gid) gid = pgid
                if (gid && aid) print gid ":" aid
            }
        ' "$pom")
        if [ -n "$gav" ]; then
            echo "$gav $rel_path" >> "$GAV_MAP_FILE"
        fi
    done < <(find "$PROJECT_ROOT" -name pom.xml -not -path '*/target/*' -print0)

    echo "$current_hash" > "$HASH_FILE"
fi

awk -v gid="$GROUP_ID" -v aid="$ARTIFACT_ID" -v map_file="$GAV_MAP_FILE" '
BEGIN {
    while ((getline line < map_file) > 0) {
        idx = index(line, " ")
        if (idx > 0) {
            key = substr(line, 1, idx - 1)
            val = substr(line, idx + 1)
            gav_to_path[key] = val
        }
    }
}
/<groupId>/ {
    match($0, /<groupId>([^<]+)<\/groupId>/, g)
    current_gid = g[1]
    source = ""
    if (match($0, /<!-- (.+) -->/, s)) source = s[1]
}
/<artifactId>/ {
    match($0, /<artifactId>([^<]+)<\/artifactId>/, a)
    current_aid = a[1]
}
/<version>/ && current_gid == gid && current_aid == aid {
    match($0, /<version>([^<]+)<\/version>/, v)
    ver = v[1]
    ver_source = ""
    if (match($0, /<!-- (.+) -->/, vs)) ver_source = vs[1]
    key = ver SUBSEP (ver_source != "" ? ver_source : source)
    if (!(key in seen)) {
        seen[key] = 1
        versions[++count] = ver
        sources[count] = (ver_source != "" ? ver_source : source)
    }
    current_gid = ""
    current_aid = ""
}

END {
    if (count == 0) {
        print "No dependency found for " gid ":" aid
        exit 1
    }
    printf "Dependency: %s:%s\n\n", gid, aid
    for (i = 1; i <= count; i++) {
        src = sources[i]
        n = split(src, src_parts, ":")
        path = ""
        if (n >= 2) path = gav_to_path[src_parts[1] ":" src_parts[2]]
        if (path) {
            printf "  Version: %-30s Declared in: %s (%s)\n", versions[i], src, path
        } else {
            printf "  Version: %-30s Declared in: %s\n", versions[i], src
        }
    }
}
' "$POM_FILE"
