#!/bin/bash

if [ "$DOCKER_HUB_TOKEN" != "" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_REPO_SLUG" == "keycloak/keycloak" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
    curl -H "Content-Type: application/json" --data '{"docker_tag": "master"}' -X POST https://registry.hub.docker.com/u/jboss/keycloak/trigger/$DOCKER_HUB_TOKEN/
    echo "Triggered Docker hub build"
elif [ "$DOCKER_HUB_TOKEN" != "" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_REPO_SLUG" == "keycloak/keycloak" ] && [ "$TRAVIS_BRANCH" == "openshift-integration2" ]; then
    curl -H "Content-Type: application/json" --data '{"docker_tag": "nightly-openshift-integration2"}' -X POST https://registry.hub.docker.com/u/jboss/keycloak/trigger/$DOCKER_HUB_TOKEN/
    echo "Triggered Docker hub build"
fi
