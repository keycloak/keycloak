package org.keycloak.testframework.database;

public class DatabaseConfigBuilder {
    private String initScript;

    private DatabaseConfigBuilder() {}

    public static DatabaseConfigBuilder create() {
        return new DatabaseConfigBuilder();
    }

    public DatabaseConfigBuilder withInitScript(String initScript) {
        this.initScript = initScript;
        return this;
    }

    public DatabaseConfig build() {
        return new DatabaseConfig(initScript);
    }
}
