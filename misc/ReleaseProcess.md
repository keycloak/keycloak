## Test

* Make sure tests pass on Travis
* Make sure tests pass on Jenkins
* Go through the (manual testing)[https://docs.google.com/spreadsheets/d/17C_WEHNE03r5DxN71OXGJaytjA6_WjZKCXRcsnmNQD4]

## Create release

* Get from github
```
$ git@github.com:keycloak/keycloak.git
```

* Build everything to make sure its kosher.
```
$ cd keycloak
$ mvn -Pjboss-release install
```

* Upload to Nexus (from project root)
```
$ mvn -Pjboss-release deploy
```

* Login to Nexus and release the maven repository uploads in the staging area.

* Upload src and distro zips to sf.net/projects/keycloak.  This includes appliance, war-dist, each adapter, and proxy distros.  You need to create an adapters folder on sf.net and each uploaded adapter there.

* Upload documentation to http://keycloak.github.io/
```
$ git clone https://github.com/keycloak/keycloak.github.io.git
$ cd keycloak.github.io.git/docs
$ rm -rf *
$ unzip distribution/docs-dist/target/keycloak-docs-1.0.0.Final.zip
$ git add --all
$ git commit
$ git push
```
* tag release
```
$ git tag -a -m "1.0.0.Final" 1.0.0.Final
$ git push --tags
```

## Update Bower
```
$ git clone https://github.com/keycloak/keycloak-js-bower
$ cp <keycloak.js from dist> dist/keycloak-js-bower
$ cp <keycloak.min.js from dist> dist/keycloak-js-bower
```
Edit bower.json and set version (include -beta -rc, but not -final). Create tag.

## Update OpenShift Cartridge

See https://github.com/keycloak/openshift-keycloak-cartridge for details

## Update Docker image

Instructions TBD

## Maven central

Releases are automatically synced to Maven central, but this can take up to one day

## Announce

* Update Magnolia site to link keycloak docs and announcements.
* Write a blog and email about release including links to download, migration guide, docs, and blurb about what's new
