#!/bin/bash

if [ "$DOCKER_HUB_TOKEN" != "" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_REPO_SLUG" == "keycloak/keycloak" ]; then
    curl -H "Content-Type: application/json" --data '{"docker_tag": "master"}' -X POST https://registry.hub.docker.com/u/jboss/keycloak/trigger/$DOCKER_HUB_TOKEN/
    echo "Triggered Docker hub build"
fi
