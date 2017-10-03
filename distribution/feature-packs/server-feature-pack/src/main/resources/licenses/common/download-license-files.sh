#!/bin/bash

set -eEu

# Dependencies: xmlstarlet, dos2unix, curl.
# Written on Fedora Linux, untested on other unixes.

xml="${1?Specify the licenses xml file. ex: rh-sso/licenses.xml}"

xml_dirname="$(dirname "$xml")"
output_dir="${LICENSE_OUTPUT_DIR:-$xml_dirname}"

echo "==> Deleting old license files in $output_dir" >&2

find "$output_dir" -maxdepth 1 '(' -type f -or -type l ')' -name '*.txt' -print0 | xargs --no-run-if-empty -0 rm

echo "==> Munging $xml to ensure Windows filename compatibility" >&2
tempfile="$(mktemp)"
trap "rm -f '$tempfile'" EXIT
xmlstarlet tr /dev/stdin "$xml" >> "$tempfile" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/ | @* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="licenseSummary/dependencies/dependency/licenses/license/name/text()">
        <xsl:value-of select="translate(., '&lt;&gt;&quot;:\\|?*', '')"/>
    </xsl:template>

    <xsl:template match="licenseSummary/others/other/licenses/license/name/text()">
        <xsl:value-of select="translate(., '&lt;&gt;&quot;:\\|?*', '')"/>
    </xsl:template>
</xsl:stylesheet>
EOF
cp "$tempfile" "$xml"

echo "==> Downloading license files for $xml into $output_dir" >&2

xmlstarlet sel -T -t -m "/licenseSummary/dependencies/dependency/licenses/license" -v "../../groupId/text()" -o $'\t' -v "../../artifactId/text()" -o $'\t' -v "../../version/text()" -o $'\t' -v "name/text()" -o $'\t' -v "url/text()" --nl "$xml" | \
while IFS=$'\t' read -r -d $'\n' groupid artifactid version name url
do
    # Windows won't like it if : is used as a separator
    filename="$groupid,$artifactid,$version,$name.txt"
    echo "$filename"
    curl -LsS -o "$output_dir/$filename" "$url"
done

xmlstarlet sel -T -t -m "/licenseSummary/others/other/licenses/license" -v "../../description/text()" -o $'\t' -v "name/text()" -o $'\t' -v "url/text()" --nl "$xml" | \
while IFS=$'\t' read -r -d $'\n' description name url
do
    # Windows won't like it if : is used as a separator
    filename="$description,$name.txt"
    echo "$filename"
    curl -LsS -o "$output_dir/$filename" "$url"
done

echo "==> Normalizing license line endings" >&2

find "$output_dir" -maxdepth 1 -type f -name '*.txt' -print0 | xargs --no-run-if-empty -0 dos2unix

echo "==> Symlinking identical files" >&2

hashtemp="$(mktemp)"
trap "rm '$hashtemp'" EXIT

cd "$output_dir"
find -maxdepth 1 -type f -name '*.txt' -print0 | LC_ALL=C sort -z | xargs --no-run-if-empty -0 sha256sum | sed 's, \./,,' > "$hashtemp"

declare -A processed_hashes

while IFS=" " read -r -d $'\n' hash filename
do
    if ! [ -v processed_hashes["$hash"] ]
    then
        echo "$filename" >&2
        grep -F "$hash " "$hashtemp" | grep -vxF "$hash $filename" | \
        while IFS=" " read -r -d $'\n' dup_hash dup_filename
        do
            echo " -> $dup_filename" >&2
            rm "$dup_filename"
            ln -s "$filename" "$dup_filename"
        done
        processed_hashes["$hash"]="$filename"
    fi
done < "$hashtemp"

echo "==> Complete" >&2
