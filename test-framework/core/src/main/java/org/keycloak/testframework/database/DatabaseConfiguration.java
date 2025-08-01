package org.keycloak.testframework.database;

public final class DatabaseConfiguration {
    private String initScript;
    private String database;
    private boolean preventReuse;

    public String getInitScript() {
        return initScript;
    }

    public void setInitScript(String initScript) {
        this.initScript = initScript;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public boolean isPreventReuse() {
        return preventReuse;
    }

    public void setPreventReuse(boolean preventReuse) {
        this.preventReuse = preventReuse;
    }
}
