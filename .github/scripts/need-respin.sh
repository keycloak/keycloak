#! /bin/bash
set -euxo pipefail

image=$1
#  Get Latest Keycloak release tag
tag=$(curl -sL http://github.com/keycloak/keycloak/releases/latest -H "Accept: application/json" | jq -r '.tag_name')

docker pull ${image}:${tag}

base_image_url=$(docker inspect ${image}:${tag} | jq -r '.[0].Config.Labels.url')

base_image_name=$(docker inspect ${image}:${tag} | jq -r '.[0].Config.Labels.url' | sed 's/.*#\///' | sed 's/\/images.*//')

docker pull ${base_image_name}:latest

latest_base_image_url=$(docker inspect ${base_image_name}:latest | jq -r '.[0].Config.Labels.url')

if [ "${base_image_url}" = "${latest_base_image_url}" ]; then
    exit -1 # no need respin
else
    exit 0
fi
