## Test

* Make sure tests pass on Travis (https://travis-ci.org/keycloak/keycloak)
* Make sure tests pass on Jenkins (https://jenkins.mw.lab.eng.bos.redhat.com/hudson/view/Keycloak/job/keycloak_all/)
* Go through the (manual testing)[https://docs.google.com/spreadsheets/d/17C_WEHNE03r5DxN71OXGJaytjA6_WjZKCXRcsnmNQD4]


## Release

### Clone from GitHub

    # git clone https://github.com/keycloak/keycloak.git
    # cd keycloak

### Prepare the release

    # mvn -Pjboss-release release:prepare

### Perform the release

    # mvn -Pjboss-release release:perform

### Deploy to Nexus

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

The OpenShift Cartridge has a base branch that is based on the WildFly cartridge and includes any changes related to Keycloak, but does not include Keycloak
itself. Any configuration changes or upgrading WildFly should be done in the base branch.

To include changes from the WildFly cartridge, for example when upgrading to a new WildFly version the base branch should be rebased on from the [wildfly-cartridge](https://github.com/openshift-cartridges/openshift-wildfly-cartridge):

    # git clone https://github.com/keycloak/openshift-keycloak-cartridge.git
    # cd openshift-keycloak-cartridge
    # git remote add wildfly https://github.com/openshift-cartridges/openshift-wildfly-cartridge.git
    # git fetch wildfly
    # git checkout base
    # git rebase wildfly
    # git push orgin base:base

To upgrade Keycloak on the cartridge run:

    # git clone https://github.com/openshift-cartridges/openshift-wildfly-cartridge.git
    # cd openshift-keycloak-cartridge

To remove the previous release of Keycloak on master run:

    # git reset --hard upstream/base

Once you've done that install the Keycloak overlay:

    # cd versions/9
    # unzip keycloak-overlay-$VERSION.zip
    # git commit -m "Install Keycloak $VERSION" -a
    # git tag $VERSION
    # git push --tags master

### Update Docker image

    # git clone https://github.com/jboss-dockerfiles/keycloak.git
    # cd keycloak

Edit server/Dockerfile and update version in `ENV KEYCLOAK_VERSION ...` line.

Edit the following files:

* server-postgres/Dockerfile
* adapter-wildfly/Dockerfile
* server-ha-postgres/Dockerfile
* server/Dockerfile
* server-mongo/Dockerfile
* examples/Dockerfile
* server-mysql/Dockerfile

And update version in `FROM jboss/keycloak:...` line.

    # git commit -m "Updated to $VERSION" -a
    # git tag $VERSION
    # git push --tags master

Go to Docker Hub. Update build settings for the following images and change `Name` and `Docker Tag Name` to the version you are releasing:

* [keycloak](https://hub.docker.com/r/jboss/keycloak/~/settings/automated-builds/)
* [adapter-wildfly](https://hub.docker.com/r/jboss/keycloak-adapter-wildfly/~/settings/automated-builds/)
* [examples](https://hub.docker.com/r/jboss/keycloak-examples/~/settings/automated-builds/)
* [postgres](https://hub.docker.com/r/jboss/keycloak-postgres/~/settings/automated-builds/)
* [mysql](https://hub.docker.com/r/jboss/keycloak-mysql/~/settings/automated-builds/)
* [mongo](https://hub.docker.com/r/jboss/keycloak-mongo/~/settings/automated-builds/)
* [ha-postgres](https://hub.docker.com/r/jboss/keycloak-ha-postgres/~/settings/automated-builds/)

Once you've updated all images. Schedule a build of the [keycloak image](https://hub.docker.com/r/jboss/keycloak/builds/). Once completed it will trigger
builds of all other images as they are linked.