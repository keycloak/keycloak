package org.keycloak.testframework.database;

public class DatabaseConfigBuilder {

    DatabaseConfiguration rep;

    private DatabaseConfigBuilder(DatabaseConfiguration rep) {
        this.rep = rep;
    }

    public static DatabaseConfigBuilder create() {
        DatabaseConfiguration rep = new DatabaseConfiguration();
        return new DatabaseConfigBuilder(rep);
    }

    public DatabaseConfigBuilder initScript(String initScript) {
        rep.setInitScript(initScript);
        return this;
    }

    public DatabaseConfigBuilder database(String database) {
        rep.setDatabase(database);
        return this;
    }

    public DatabaseConfigBuilder preventReuse(boolean preventReuse) {
        rep.setPreventReuse(preventReuse);
        return this;
    }

    public DatabaseConfiguration build() {
        return rep;
    }
}
