#!/bin/bash

start_mssql_container() {
  CONTAINER_NAME=mssql

  docker stop $CONTAINER_NAME
  docker rm $CONTAINER_NAME

  export INIT_CMD='-- Allow user authentication by database
      EXEC sp_configure "CONTAINED DATABASE AUTHENTICATION", 1
      GO
      RECONFIGURE WITH OVERRIDE
      GO
      USE master
      GO
      -- Enable XA transactions (required by quarkus reactive data sources)
      PRINT "Installing XA extensions"
      EXEC sp_sqljdbc_xa_install
      GO
      PRINT "Creating keycloak DB"
      CREATE DATABASE keycloak CONTAINMENT = PARTIAL
      GO
      USE keycloak
      GO
      PRINT "Creating login"
      CREATE LOGIN keycloak WITH PASSWORD= "keycl0@k", DEFAULT_DATABASE=[keycloak]
      GO
      PRINT "Creating keycloak user"
      CREATE USER keycloak FOR LOGIN keycloak
      GO
      PRINT "Setting keycloak user as db owner"
      ALTER ROLE db_owner ADD MEMBER keycloak
      GO
      USE master
      GO
      PRINT "Granting db accesss to keycloak user"
      EXEC sp_grantdbaccess "keycloak", "keycloak"
      GO
      -- This role is required to use XA transactions
      PRINT "Adding role SqlJDBCXAUser to keycloak user"
      EXEC sp_addrolemember [SqlJDBCXAUser], "keycloak"
      GO'

  echo "Starting mssql container"
  docker run --name $CONTAINER_NAME \
            -e 'ACCEPT_EULA=Y' \
            -e "SA_PASSWORD=ms5ql@dmin" \
            -p 1433:1433 \
            -e "INIT_CMD=$INIT_CMD" \
            -d \
            mcr.microsoft.com/mssql/server:2019-CU14-ubuntu-20.04
  sleep 2
  docker exec -it $CONTAINER_NAME /bin/sh -c 'echo "$INIT_CMD" > /tmp/init-mssql.sql'
  echo "Waiting for mssql container to start up"
  sleep 10
  docker exec -it $CONTAINER_NAME /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "ms5ql@dmin" -i /tmp/init-mssql.sql
  echo "mssql container intialized"
}

rebuild() {
  ./mvnw -s maven-settings.xml clean install -nsu -B -e -DskipTests -Pdistribution,quarkus
  ./mvnw -s maven-settings.xml compile install  -DskipTests=true -f themes/pom.xml
  ./mvnw -s maven-settings.xml clean install -nsu -B -e -f testsuite/integration-arquillian/servers/auth-server -Pauth-server-quarkus,auth-server-undertow,db-mssql
  ./mvnw -s maven-settings.xml clean install -nsu -B -e -f testsuite/integration-arquillian/servers/auth-server/services/testsuite-providers -Pauth-server-quarkus,auth-server-undertow,db-mssql
  ./mvnw -s maven-settings.xml clean install -nsu -B -e -f testsuite/integration-arquillian/tests/base/pom.xml -DskipTests=true -Pauth-server-quarkus,auth-server-undertow,db-mssql
}

if [ "-build" == "$1" ]; then
  rebuild
fi

start_mssql_container

echo "Starting test suite"

./mvnw  -s maven-settings.xml install  -f testsuite/integration-arquillian/tests/base/pom.xml \
    -Pauth-server-quarkus -Dpageload.timeout=90000 \
    -Pmap-storage -Pmap-storage-jpa \
    -Dkeycloak.map.storage.connectionsJpa.url="jdbc:sqlserver://localhost:1433;database=keycloak;trustServerCertificate=true" \
    -Dkeycloak.map.storage.connectionsJpa.user=keycloak \
    -Dkeycloak.map.storage.connectionsJpa.password=keycl0@k \
    -Dkeycloak.map.storage.connectionsJpa.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver \
    -Dkeycloak.storage.connections.vendor=mssql \
    -Dkeycloak.connectionsJpa.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver \
    -Dkeycloak.connectionsJpa.database=keycloak \
    -Dkeycloak.connectionsJpa.user=keycloak \
    -Dkeycloak.connectionsJpa.password=keycl0@k \
    -Dkeycloak.connectionsJpa.url="jdbc:sqlserver://localhost:1433;database=keycloak;trustServerCertificate=true" \
    -Djdbc.mvn.groupId=com.microsoft.sqlserver \
    -Djdbc.mvn.artifactId=mssql-jdbc \
    -Djdbc.mvn.version=9.2.0.jre8 \
    -Dauth.server.quarkus=true \
    -Dauth.server.container=auth-server-quarkus \
    -DdroneInstantiationTimeoutInSeconds=60 \

# Settings useful for debugging:
#
#    -DXXmaven.surefire.debug \
#    -Dkeycloak.map.storage.connectionsJpa.showSql=true \
# -Dtest='BrowserFlowTest#testUserWithoutAdditionalFactorConnection,VerifyProfileTest#testDisplayName'

