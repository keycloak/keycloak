#!/bin/bash

BRANCHES="upstream/master prod/3.4.x prod/4.8.x"

echo "-----------------------------------------------------------------"

echo "Fetching remotes"
echo ""

git fetch upstream
git fetch gitlab
git fetch prod

echo "-----------------------------------------------------------------"

for i in $BRANCHES; do
	TARGET_BRANCH=`echo $i | cut -d '/' -f 2`

	echo "Syncing $i to gitlab/$TARGET_BRANCH"
	echo ""

	git branch | grep " tmp-$TARGET_BRANCH$" &>/dev/null && git branch -D tmp-$TARGET_BRANCH
	git checkout $i -b tmp-$TARGET_BRANCH
	git push gitlab tmp-$TARGET_BRANCH:$TARGET_BRANCH
	git checkout master	
	git branch -D tmp-$TARGET_BRANCH

	echo "-----------------------------------------------------------------"
done
