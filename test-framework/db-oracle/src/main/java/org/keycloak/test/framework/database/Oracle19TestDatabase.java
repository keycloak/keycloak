package org.keycloak.test.framework.database;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

class Oracle19TestDatabase extends AbstractContainerTestDatabase {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("docker.io/miquelsi/oracle-19c:19.3").asCompatibleSubstituteFor("gvenzl/oracle-free");

    @SuppressWarnings("resource")
    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new OracleContainer(IMAGE_NAME)
                .withEnv(Map.of("ORACLE_SID", "keycloak", "ORACLE_PWD", "sa"));
    }

    @Override
    public String getPostStartCommand() {
        return "(echo 'alter session set \"_ORACLE_SCRIPT\"=true;' && " +
                "echo 'GRANT CONNECT,RESOURCE,DBA,GRANT ANY PRIVILEGE,UNLIMITED TABLESPACE TO test;')" +
                " | sqlplus -L SYS/sa" + "@localhost/keycloak AS SYSDBA";
    }

    @Override
    public String getKeycloakDatabaseName() {
        return "oracle";
    }
}
