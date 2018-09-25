#!/bin/bash

git fetch upstream
git fetch gitlab
git fetch prod

git push gitlab upstream/master:master
git push gitlab prod/rhsso-7.2.x:rhsso-7.2.x
