# Keycloak Arquillian WebSphere AS Integration Testsuite

- arquillian-was-remote-8.5-custom container is used for deploying artifacts to running WebSphere server
- arquillian-was-remote-8.5-custom is based on arquillian-was-remote-8.5 and solves some ibm dependency issues
- arquillian-was-remote-8.5-custom can be downloaded from this [repo](https://repository.jboss.org/nexus/content/repositories/jboss_releases_staging_profile-11801)
- more info about arquillian-was-remote-8.5-custom: 
    - There is the [artifact](https://github.com/vramik/arquillian-container-was/blob/custom/was-remote-8.5/pom.xml#L17)
    - This is a [profile](https://github.com/vramik/arquillian-container-was/blob/custom/pom.xml#L108-L114) to activate
    - To build `ws-dependencies` module it is required to specify `lib_location` property where directory `lib` is located. The `lib` has to contain `com.ibm.ws.admin.client_8.5.0.jar` and `com.ibm.ws.orb_8.5.0.jar` which are part of WebSphere AS installation
        - see [pom.xml](https://github.com/vramik/arquillian-container-was/blob/custom/ws-dependencies/pom.xml) for more details
        - note: to solve classpath conflicts the package javax/ws from within `com.ibm.ws.admin.client_8.5.0.jar` has to be removed

## How to run tests

1. start IBM WebSphere container with ibmjdk8 (tests expects that app-server runs on port 8280)
2. add the [repository](https://repository.jboss.org/nexus/content/repositories/jboss_releases_staging_profile-12222) to settings.xml 
3. mvn -f keycloak/pom.xml -Pdistribution -DskipTests clean install
4. mvn -f keycloak/testsuite/integration-arquillian/pom.xml -Pauth-server-wildfly -DskipTests clean install
5. mvn -f keycloak/testsuite/integration-arquillian/tests/other/adapters/was/pom.xml -Pauth-server-wildfly,app-server-was clean install