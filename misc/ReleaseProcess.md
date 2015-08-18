## Test

* Make sure tests pass on Travis (https://travis-ci.org/keycloak/keycloak)
* Make sure tests pass on Jenkins (https://jenkins.mw.lab.eng.bos.redhat.com/hudson/view/Keycloak/job/keycloak_all/)
* Go through the (manual testing)[https://docs.google.com/spreadsheets/d/17C_WEHNE03r5DxN71OXGJaytjA6_WjZKCXRcsnmNQD4]


## Release

*Releasing currently requires using JDK 7 due to a bug in JAX-RS Doclets*

### Clone from GitHub

    # git clone https://github.com/keycloak/keycloak.git
    # cd keycloak

### Update version

    # mvn versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false -Pjboss-release

### Build

    # mvn install install -Pdistribution
    # mvn install -Pjboss-release -DskipTests

### Tag

    # git tag $VERSION
    # git push --tags

### Deploy to Nexus

    # mvn deploy -DskipTests -Pjboss-release

Then login to Nexus and release the maven uploads in the staging area. Artifacts will eventually be synced to Maven Central, but this can take up to 24 hours.

### Upload

Upload all artifacts to downloads.jboss.org (see https://mojo.redhat.com/docs/DOC-81955 for more details):

    # rsync -rv --protocol=28 distribution/downloads/target/$VERSION keycloak@filemgmt.jboss.org:/downloads_htdocs/keycloak

### Upload documentation

    # git clone https://github.com/keycloak/keycloak.github.io.git
    # cd keycloak.github.io
    # rm -rf docs
    # unzip ../distribution/downloads/target/$VERSION/keycloak-docs-$VERSION.zip
    # mv keycloak-docs-$VERSION docs
    # git commit -m "Updated docs to $VERSION"
    # git tag $VERSION
    # git push --tags


## After Release

### Update Bower

    # git clone https://github.com/keycloak/keycloak-js-bower
    # cd keycloak-js-bower
    # unzip ../distribution/downloads/target/$VERSION/adapters/keycloak-js-adapter-dist-$VERSION.zip
    # mv keycloak-js-adapter-dist-$VERSION/*.js dist/keycloak-js-bower
    # rmdir keycloak-js-adapter-dist-$VERSION

Edit bower.json and set version (include -beta -rc, but not -final). Then commit create tag:

    # git commit -m "Updated to $VERSION"
    # git tag $VERSION
    # git push --tags

### Update Website

* Edit [Docs page](https://www.jboss.org/author/keycloak/docs.html) and update version
* Edit [Downloads page](https://www.jboss.org/author/keycloak/downloads) edit directory listing component and update version in title and project root

### Announce release

Write a blog post on blog.keycloak.org, blurb about what's new and include links to website for download and jira for changes.

Copy blog post and send to keycloak-dev and keycloak-users mailing lists.

Post link to blog post on Twitter (with Keycloak user).

### Update OpenShift Cartridge

Instructions TBD

### Update Docker image

Instructions TBD