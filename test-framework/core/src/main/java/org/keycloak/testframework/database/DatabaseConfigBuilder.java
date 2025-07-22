package org.keycloak.testframework.database;

public class DatabaseConfigBuilder {

    DatabaseConfigRepresentation rep;

    private DatabaseConfigBuilder(DatabaseConfigRepresentation rep) {
        this.rep = rep;
    }

    public static DatabaseConfigBuilder create() {
        DatabaseConfigRepresentation rep = new DatabaseConfigRepresentation();
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

    public DatabaseConfigRepresentation build() {
        return rep;
    }
}
