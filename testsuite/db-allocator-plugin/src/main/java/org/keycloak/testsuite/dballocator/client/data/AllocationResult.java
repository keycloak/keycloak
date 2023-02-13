package org.keycloak.testsuite.dballocator.client.data;

import java.util.Properties;

public class AllocationResult {

    private final String uuid;
    private final String driver;
    private final String database;
    private final String user;
    private final String password;
    private final String url;

    private AllocationResult(String uuid) {
        this.uuid = uuid;
        this.driver = null;
        this.database = null;
        this.user = null;
        this.password = null;
        this.url = null;
    }

    private AllocationResult(String uuid, String driver, String database, String user, String password, String url) {
        this.uuid = uuid;
        this.driver = driver;
        this.database = database;
        this.user = user;
        this.password = password;
        this.url = url;
    }

    public static AllocationResult forRelease(String uuid) {
        return new AllocationResult(uuid);
    }

    public static AllocationResult successful(Properties properties) {
        return new AllocationResult(
                properties.getProperty("uuid"),
                properties.getProperty("db.jdbc_class"),
                properties.getProperty("db.name"),
                properties.getProperty("db.username"),
                properties.getProperty("db.password"),
                properties.getProperty("db.jdbc_url"));
    }

    public String getDriver() {
        return driver;
    }

    public String getDatabase() {
        return database;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getURL() {
        return url;
    }

    public String getUUID() {
        return uuid;
    }

    @Override
    public String toString() {
        return "AllocationResult{" +
                "uuid='" + uuid + '\'' +
                ", driver='" + driver + '\'' +
                ", database='" + database + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
