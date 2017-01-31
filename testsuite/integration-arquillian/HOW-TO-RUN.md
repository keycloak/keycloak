How To Run various testsuite configurations
===========================================

## Base steps

It's recomended to build the workspace including distribution.

````
cd $KEYCLOAK_SOURCES
mvn clean install -DskipTests=true
cd distribution
mvn clean install
````

## Run adapter tests

### Wildfly

````
# Prepare servers
mvn -f testsuite/integration-arquillian/servers/pom.xml clean install \
 -Pauth-server-wildfly \
 -Papp-server-wildfly

# Run tests
mvn -f testsuite/integration-arquillian/tests/other/adapters/jboss/wildfly/pom.xml \
 clean install \
 -Pauth-server-wildfly \
 -Papp-server-wildfly
````

### JBoss Fuse 6.3
----------------------------------------
1) Download JBoss Fuse 6.3 to your filesystem. It can be downloaded from http://origin-repository.jboss.org/nexus/content/groups/m2-proxy/org/jboss/fuse/jboss-fuse-karaf 
Assumed you downloaded `jboss-fuse-karaf-6.3.0.redhat-229.zip`

2) Install to your local maven repository and change the properties according to your env (This step can be likely avoided if you somehow configure your local maven settings to point directly to Fuse repo):

````
mvn install:install-file \
 -DgroupId=org.jboss.fuse \
 -DartifactId=jboss-fuse-karaf \
 -Dversion=6.3.0.redhat-229 \
 -Dpackaging=zip \
 -Dfile=/mydownloads/jboss-fuse-karaf-6.3.0.redhat-229.zip
````

3) Prepare Fuse and run the tests (change props according to your environment, versions etc):

````
# Prepare Fuse server
mvn -f testsuite/integration-arquillian/servers \
 clean install \
 -Pauth-server-wildfly \
 -Papp-server-fuse63 \
 -Dfuse63.version=6.3.0.redhat-229 \
 -Dapp.server.karaf.update.config=true \
 -Dmaven.local.settings=$HOME/.m2/settings.xml \
 -Drepositories=,http://download.eng.bos.redhat.com/brewroot/repos/sso-7.1-build/latest/maven/ \
 -Dmaven.repo.local=$HOME/.m2/repository
 
# Run the Fuse adapter tests
mvn -f testsuite/integration-arquillian/tests/other/adapters/karaf/fuse63/pom.xml \
 clean install \
 -Pauth-server-wildfly \
 -Papp-server-fuse63 \
 -Dfuse63.version=6.3.0.redhat-229
````

### EAP6 with Hawtio

1) Download JBoss EAP 6.4.0.GA zip

2) Install to your local maven repository and change the properties according to your env (This step can be likely avoided if you somehow configure your local maven settings to point directly to EAP repo):

````
mvn install:install-file \
 -DgroupId=org.jboss.as \
 -DartifactId=jboss-as-dist \
 -Dversion=7.5.0.Final-redhat-21 \
 -Dpackaging=zip \
 -Dfile=/mydownloads/jboss-eap-6.4.0.zip
````

3) Download Fuse EAP installer (for example from http://origin-repository.jboss.org/nexus/content/groups/m2-proxy/com/redhat/fuse/eap/fuse-eap-installer/6.3.0.redhat-220/ )

4) Install previously downloaded file manually

````
mvn install:install-file \
 -DgroupId=com.redhat.fuse.eap \
 -DartifactId=fuse-eap-installer \
 -Dversion=6.3.0.redhat-220 \
 -Dpackaging=jar \
 -Dfile=/fuse-eap-installer-6.3.0.redhat-220.jar
````

5) Prepare EAP6 with Hawtio and run the test

````
# Prepare EAP6 and deploy hawtio
mvn -f testsuite/integration-arquillian/servers \
 clean install \
 -Pauth-server-wildfly \
 -Papp-server-eap6-fuse \
 -Dapp.server.jboss.version=7.5.0.Final-redhat-21 \
 -Dfuse.installer.version=6.3.0.redhat-220
 
# Run the test
mvn -f testsuite/integration-arquillian/tests/other/adapters/jboss/eap6-fuse/pom.xml \
  clean install \
  -Pauth-server-wildfly \
  -Papp-server-eap6-fuse  
```` 

## Migration test

### DB migration test

This test will:
 - start Keycloak 1.9.8
 - import realm and some data to MySQL DB
 - stop Keycloak 1.9.8
 - start latest KEycloak, which automatically updates DB from 1.9.8
 - Do some test that data are correct
 

1) Prepare MySQL DB and ensure that MySQL DB is empty. See [../../misc/DatabaseTesting.md](../../misc/DatabaseTesting.md) for some hints for locally prepare Docker MySQL image.

2) Run the test (Update according to your DB connection, versions etc):

````
export DB_HOST=localhost

mvn -f testsuite/integration-arquillian/pom.xml \
  clean install \
  -Pauth-server-wildfly,jpa,clean-jpa,auth-server-migration \
  -Dtest=MigrationTest \
  -Dmigration.mode=auto \
  -Dmigrated.auth.server.version=1.9.8.Final \
  -Djdbc.mvn.groupId=mysql \
  -Djdbc.mvn.version=5.1.29 \
  -Djdbc.mvn.artifactId=mysql-connector-java \
  -Dkeycloak.connectionsJpa.url=jdbc:mysql://$DB_HOST/keycloak \
  -Dkeycloak.connectionsJpa.user=keycloak \
  -Dkeycloak.connectionsJpa.password=keycloak
````

### JSON export/import migration test
This will start latest Keycloak and import the realm JSON file, which was previously exported from Keycloak 1.9.8.Final
  
````
mvn -f testsuite/integration-arquillian/pom.xml \
  clean install \
  -Pauth-server-wildfly,migration-import \
  -Dtest=MigrationTest \
  -Dmigration.mode=import \
  -Dmigrated.auth.server.version=1.9.8.Final
````




