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

    /**
     * Configure a script to initialise the database on startup
     *
     * @param initScript path to init script on the classpath
     * @return
     */
    public DatabaseConfigBuilder initScript(String initScript) {
        rep.setInitScript(initScript);
        return this;
    }

    /**
     * Set the database name to use, defaults to <code>keycloak</code>
     *
     * @param database name of the database to use
     * @return
     */
    public DatabaseConfigBuilder database(String database) {
        rep.setDatabase(database);
        return this;
    }

    /**
     * Prevent re-use of the database
     *
     * @param preventReuse set to <code>true</code> to prevent re-use of the database
     * @return
     */
    public DatabaseConfigBuilder preventReuse(boolean preventReuse) {
        rep.setPreventReuse(preventReuse);
        return this;
    }

    public DatabaseConfiguration build() {
        return rep;
    }
}
