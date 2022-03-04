#!/bin/sh

LATEST_RELEASE_URL=$(curl -s https://api.github.com/repos/github/codeql-cli-binaries/releases/latest | grep browser_download_url | cut -d '"' -f 4 | grep -i linux)

wget -q --show-progress "$LATEST_RELEASE_URL"
unzip codeql-linux64.zip
