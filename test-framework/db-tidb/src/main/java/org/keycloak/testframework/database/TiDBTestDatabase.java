package org.keycloak.testframework.database;

import org.keycloak.testframework.util.ContainerImages;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.tidb.TiDBContainer;
import org.testcontainers.utility.DockerImageName;

class TiDBTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(TiDBTestDatabase.class);

    public static final String NAME = "tidb";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new TiDBContainer(DockerImageName.parse(ContainerImages.getContainerImageName(NAME)).asCompatibleSubstituteFor("pingcap/tidb")){
            @Override
            public TiDBContainer withDatabaseName(String databaseName) {
                if(StringUtils.equals(this.getDatabaseName(), databaseName)) {
                    return this;
                }
                throw new UnsupportedOperationException("The TiDB docker image does not currently support this");
            }

            @Override
            public TiDBContainer withUsername(String username) {
                if(StringUtils.equals(this.getUsername(), username)) {
                    return this;
                }
                throw new UnsupportedOperationException("The TiDB docker image does not currently support this");
            }

            @Override
            public TiDBContainer withPassword(String password) {
                if(StringUtils.equals(this.getPassword(), password)) {
                    return this;
                }
                throw new UnsupportedOperationException("The TiDB docker image does not currently support this");
            }
        }.withExposedPorts(4000);
    }

    @Override
    public String getDatabaseVendor() {
        return "tidb";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }


    @Override
    public String getDatabase() {
        return "test";
    }

    @Override
    public String getUsername() {
        return "root";
    }

    @Override
    public String getPassword() {
        return "";
    }
}
