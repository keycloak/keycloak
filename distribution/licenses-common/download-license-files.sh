#!/bin/bash

set -eEu

# Dependencies: xmlstarlet, dos2unix, curl.
# Written on Fedora Linux, untested on other unixes.

xml="${1?Specify the licenses xml file. ex: rh-sso/licenses.xml}"

xml_dirname="$(dirname "$xml")"
output_dir="${LICENSE_OUTPUT_DIR:-$xml_dirname}"

echo "==> Checking license data for errors" >&2

die() {
    { set +x; } 2>/dev/null
    echo "$@" >&2
    exit 1
}

set -x

xmlstarlet val "$xml" || die "Specified license data is not valid XML"
grep -hn '/blob/' "$xml" && die "Some licence URLs reference HTML (/blob/) files, replace them with raw"

{ set +x; } 2>/dev/null

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

    <xsl:template match="licenseSummary/others/other/description/text()">
        <xsl:value-of select="translate(., '/', '-')"/>
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
    curl -LfsS -o "$output_dir/$filename" "$url"
done

xmlstarlet sel -T -t -m "/licenseSummary/others/other/licenses/license" -v "../../description/text()" -o $'\t' -v "name/text()" -o $'\t' -v "url/text()" --nl "$xml" | \
while IFS=$'\t' read -r -d $'\n' description name url
do
    # Windows won't like it if : is used as a separator
    filename="$description,$name.txt"
    echo "$filename"
    curl -LfsS -o "$output_dir/$filename" "$url"
done

echo "==> Normalizing license line endings" >&2

find "$output_dir" -maxdepth 1 -type f -name '*.txt' -print0 | xargs --no-run-if-empty -0 dos2unix

echo "==> Complete" >&2
