package org.keycloak.models.mongo.utils;

/**
 * Encapsulates all info about configuration of MongoDB instance
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoConfiguration {

    private final String host;
    private final int port;
    private final String dbName;

    private final boolean clearCollectionsOnStartup;
    private final boolean startEmbedded;

    public MongoConfiguration(String host, int port, String dbName, boolean clearCollectionsOnStartup, boolean startEmbedded) {
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.clearCollectionsOnStartup = clearCollectionsOnStartup;
        this.startEmbedded = startEmbedded;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDbName() {
        return dbName;
    }

    public boolean isClearCollectionsOnStartup() {
        return clearCollectionsOnStartup;
    }

    public boolean isStartEmbedded() {
        return startEmbedded;
    }

    @Override
    public String toString() {
        return String.format("MongoConfiguration: host: %s, port: %d, dbName: %s, clearCollectionsOnStartup: %b, startEmbedded: %b",
                host, port, dbName, clearCollectionsOnStartup, startEmbedded);
    }
}
