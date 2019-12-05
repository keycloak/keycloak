#!/bin/bash
set -e

[[ $TRAVIS_SECURE_ENV_VARS == "true" ]] || { echo "No github key avaliable, aborting publishing"; exit 0; }

ID_REF="$(git rev-parse --short HEAD)"

git clone "https://${GH_KEY}@${GH_REF}" ./docs-out -b ${GH_PAGES_BRANCH} --single-branch --depth=1

cd docs-out

# clear out everything
git rm -rf .
git clean -fxd

# get new content
cp ../docs-built/* . -R

git add .

# inside this git repo we'll pretend to be a new user
git config user.name "Travis CI"
git config user.email "travisci@users.noreply.github.com"

# The first and only commit to this new Git repo contains all the
# files present with the commit message "Deploy to GitHub Pages".
git commit -m "docs(*): new deploy (angular-ui/ui-select@${ID_REF})"


git push origin --quiet
#> /dev/null 2>&1
