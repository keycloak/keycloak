package org.keycloak.models.mongo.utils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SystemPropertiesConfigurationProvider {

    private static final String MONGO_HOST = "keycloak.mongo.host";
    private static final String MONGO_PORT = "keycloak.mongo.port";
    private static final String MONGO_DB_NAME = "keycloak.mongo.db";
    private static final String MONGO_CLEAR_COLLECTIONS_ON_STARTUP = "keycloak.mongo.clearCollectionsOnStartup";
    private static final String MONGO_START_EMBEDDED = "keycloak.mongo.startEmbedded";

    // Port where MongoDB instance is normally started on linux. This port should be used if we're not starting embedded instance
    private static final int MONGO_DEFAULT_PORT = 27017;

    // Port where embedded MongoDB instance will be started. Same port will be used by KeycloakApplication then
    public static final int MONGO_DEFAULT_PORT_EMBEDDED = 27018;

    public static String getMongoHost() {
        return System.getProperty(MONGO_HOST, "localhost");
    }

    public static int getMongoPort() {
        String portProp = System.getProperty(MONGO_PORT);
        if (portProp != null) {
            return Integer.parseInt(portProp);
        } else {
            // Default port is 27017 in case of non-embedded, and 27018 in case of embedded
            return isStartEmbedded() ? MONGO_DEFAULT_PORT_EMBEDDED : MONGO_DEFAULT_PORT;
        }
    }

    public static String getMongoDbName() {
        return System.getProperty(MONGO_DB_NAME, "keycloak");
    }

    public static boolean isClearCollectionsOnStartup() {
        return Boolean.parseBoolean(System.getProperty(MONGO_CLEAR_COLLECTIONS_ON_STARTUP, "true"));
    }

    public static boolean isStartEmbedded() {
        return Boolean.parseBoolean(System.getProperty(MONGO_START_EMBEDDED, "false"));
    }

    // Create configuration based on system properties
    public static MongoConfiguration createConfiguration() {
        return new MongoConfiguration(
                getMongoHost(),
                getMongoPort(),
                getMongoDbName(),
                isClearCollectionsOnStartup(),
                isStartEmbedded()
        );
    }
}
