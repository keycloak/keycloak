package org.keycloak.testframework.database;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class KeycloakEnterpriseDbContainer extends JdbcDatabaseContainer<KeycloakEnterpriseDbContainer> {
    private String databaseName = "keycloak";
    private String username = "enterprisedb";
    private String password = "password";
    private static final int PORT = 5432;

    public KeycloakEnterpriseDbContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", getHost(), getMappedPort(PORT), getDatabaseName());
    }

    @Override
    protected void configure() {
        addEnv("PGDATABASE", getDatabaseName());
        addEnv("PGUSER", getUsername());
        addEnv("PGPASSWORD", getPassword());
        addExposedPort(PORT);
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public KeycloakEnterpriseDbContainer withUsername(String username) {
        this.username = username;
        return this;
    }

    @Override
    public KeycloakEnterpriseDbContainer withPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public KeycloakEnterpriseDbContainer withDatabaseName(String dbName) {
        this.databaseName = dbName;
        return this;
    }

    @Override
    public KeycloakEnterpriseDbContainer withUrlParam(String paramName, String paramValue) {
        throw new UnsupportedOperationException();
    }
}
