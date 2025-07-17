package org.keycloak.testframework.database;

public class DatabaseConfigBuilder {
    private String initScript;
    private String database;
    private boolean preventReuse;

    private DatabaseConfigBuilder() {}

    public static DatabaseConfigBuilder create() {
        return new DatabaseConfigBuilder();
    }

    public DatabaseConfigBuilder withInitScript(String initScript) {
        this.initScript = initScript;
        return this;
    }

    public DatabaseConfigBuilder withDatabase(String database) {
        this.database = database;
        return this;
    }

    public DatabaseConfigBuilder withPreventReuse(boolean preventReuse) {
        this.preventReuse = preventReuse;
        return this;
    }

    public DatabaseConfig build() {
        return new DatabaseConfig(initScript, database, preventReuse);
    }
}
