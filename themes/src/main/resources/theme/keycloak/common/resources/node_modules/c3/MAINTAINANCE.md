# Release process

If you don't have `bmp` command installed, first install `bmp` ruby gem:

    gem install bmp

When master is ready for the next release, hit the command:

    bmp -p

This automatically updates all the version numbers with a new one in the repository.

Then hit the command:

    npm run dist

This builds the scripts and stylesheets. Then hit:

    bmp -c

This commits all the changes (including the built assets) and git-tags a new version (like v0.4.16):

Then publish it to the npm registry (you need admin access to c3 module):

    npm publish

At this point, the new version is available through npm.

Then push master and the tag to github:

    git push origin master vX.Y.Z

That's all.
