Keycloak Feature Pack Builds
============================

The feature pack builds rely on WildFly Feature packs to create different combinations
of Keycloak server/adapter and WildFly web/full.

Types of Builds Created
--------------------
The three directories under feature-pack-builds are _adapter-only_, _server-only_, and _server-and-adapter_.

* **adapter-only** - A WildFly server with the Keycloak adapter subsystem added.  This build is based on the WildFly Web Feature Pack.  **keycloak-adapter-feature-pack** contains all the modules needed to run the Keycloak WildFly Adapter.  Therefore, the build is _org.wildfly:wildfly-web-feature-pack_ + _org.keycloak:keycloak-adapter-feature-pack_.
* **server-only** - A WildFly server with the Keycloak adapter subsystem added.  This build is based on the WildFly Full Feature Pack.  **keycloak-server-feature-pack** contains all the modules needed to run the Keycloak Server without those already provided by the full WildFly Server.  Therefore, the build is _org.wildfly:wildfly-feature-pack_ + _org.keycloak:keycloak-server-feature-pack_.
* **server-and-adapter** is the same thing as **server-only** except it also includes **keycloak-adapter-feature-pack**.  Therefore, the build is  _org.wildfly:wildfly-feature-pack_ + _org.keycloak:keycloak-server-feature-pack_ + _org.keycloak:keycloak-adapter-feature-pack_.

Building
--------
Each of the three types of builds in turn creates a build version and a dist version.  

The build version is a server that uses the new WildFly 9 feature whereby maven artifacts are not bundled with the server.  Instead, they are looked up in a repository.  

For the dist version, these artifacts are copied into the server itself and no maven repo is required.

By default, the dist version does not get built.  To build it, specify the jboss-release profile:

_mvn install -Pjboss-release_
