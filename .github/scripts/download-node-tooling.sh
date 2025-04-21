#!/bin/bash -e

abort() {
  echo $1
  exit 1
}

download_file() {
    local url=$1
    local target=$2

    echo "Downloading $(basename "$url")..."
    mkdir -p "$(dirname "$target")"
    curl --silent --fail --show-error --retry 3 --retry-delay 30 --output "$target" "$url"
}

node_url() {
    local version=$1
    local platform=$2
    local root_url="https://nodejs.org/dist/v$version"

    if [ "$platform" == "windows" ]; then
        echo "$root_url/win-x64/node.exe"
    elif [ "$platform" == "linux" ]; then
        echo "$root_url/node-v$version-linux-x64.tar.gz"
    else
        abort "Unsupported platform: $platform"
    fi
}

pnpm_url() {
    local version=$1

    echo "https://registry.npmjs.org/pnpm/-/pnpm-$version.tgz"
}

main() {
    local node_version=$1
    local pnpm_version=$2

    if [ "$node_version" == "" ] || [ "$pnpm_version" == "" ]; then
        abort "Usage: download-node-tooling.sh <NODE_VERSION> <PNPM_VERSION>"
    fi

    local target_directory=~/.m2/repository/com/github/eirslett

    download_file "$(node_url "$node_version" "linux")" "$target_directory/node/$node_version/node-$node_version-linux-x64.tar.gz"
    download_file "$(node_url "$node_version" "windows")" "$target_directory/node/$node_version/node-$node_version-win-x64.exe"
    download_file "$(pnpm_url "$pnpm_version")" "$target_directory/pnpm/$pnpm_version/pnpm-$pnpm_version.tar.gz"
}

main "$1" "$2"
