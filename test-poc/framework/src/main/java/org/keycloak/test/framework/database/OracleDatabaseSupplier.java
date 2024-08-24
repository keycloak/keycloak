package org.keycloak.test.framework.database;

import java.util.HashMap;
import java.util.Map;

public class OracleDatabaseSupplier extends AbstractDatabaseSupplier {

    public static final String VENDOR = "oracle";
    private static final String CONTAINER_IMAGE = "docker.io/miquelsi/oracle-19c:19.3";

    @Override
    TestDatabase getTestDatabase() {
        Map<String, String> env = new HashMap<>();
        env.put("ORACLE_SID", "keycloak");
        env.put("ORACLE_PWD", "sa");
        DatabaseConfig databaseConfig = new DatabaseConfig()
                .vendor(VENDOR)
                .username(DEFAULT_DB_USERNAME)
                .password(DEFAULT_DB_PASSWORD)
                .postStartCommand("(echo 'alter session set \"_ORACLE_SCRIPT\"=true;' && echo 'CREATE USER " +
                        DEFAULT_DB_USERNAME + " IDENTIFIED BY \"" +
                        DEFAULT_DB_PASSWORD + "\";' && echo 'GRANT CONNECT,RESOURCE,DBA,GRANT ANY PRIVILEGE,UNLIMITED TABLESPACE TO " +
                        DEFAULT_DB_USERNAME + ";') | sqlplus -L SYS/" + env.get("ORACLE_PWD") + "@localhost/" + env.get("ORACLE_SID") + " AS SYSDBA")
                .containerImage(CONTAINER_IMAGE)
                .env(env);

        return new TestDatabase(databaseConfig);
    }

    @Override
    public String getAlias() {
        return VENDOR;
    }
}
