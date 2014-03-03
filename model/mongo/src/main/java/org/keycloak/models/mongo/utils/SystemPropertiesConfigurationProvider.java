package org.keycloak.models.mongo.utils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SystemPropertiesConfigurationProvider {

    private static final String MONGO_HOST = "keycloak.mongo.host";
    private static final String MONGO_PORT = "keycloak.mongo.port";
    private static final String MONGO_DB_NAME = "keycloak.mongo.db";
    private static final String MONGO_CLEAR_ON_STARTUP = "keycloak.mongo.clearOnStartup";

    // Property names from Liveoak . Those are used as fallback in case that original value is not available
    private static final String MONGO_HOST_2 = "mongo.host";
    private static final String MONGO_PORT_2 = "mongo.port";
    private static final String MONGO_DB_NAME_2 = "mongo.db";
    private static final String MONGO_CLEAR_ON_STARTUP_2 = "mongo.clearCollectionsOnStartup";

    // Port where MongoDB instance is normally started on linux. This port should be used if we're not starting embedded instance
    private static final String MONGO_DEFAULT_PORT = "27017";

    public static String getMongoHost() {
        return getSystemPropertyWithFallback(MONGO_HOST, MONGO_HOST_2, "localhost");
    }

    public static int getMongoPort() {
        String portProp = getSystemPropertyWithFallback(MONGO_PORT, MONGO_PORT_2, MONGO_DEFAULT_PORT);
        return Integer.parseInt(portProp);
    }

    public static String getMongoDbName() {
        return getSystemPropertyWithFallback(MONGO_DB_NAME, MONGO_DB_NAME_2, "keycloak");
    }

    public static boolean isClearCollectionsOnStartup() {
        String property = getSystemPropertyWithFallback(MONGO_CLEAR_ON_STARTUP, MONGO_CLEAR_ON_STARTUP_2, "false");
        return "true".equalsIgnoreCase(property);
    }

    // Check if property propName1 (like "keycloak.mongo.host" is available and if not, then fallback to property "mongo.host" )
    private static String getSystemPropertyWithFallback(String propName1, String propName2, String defaultValue) {
        String propValue1 = System.getProperty(propName1);
        return propValue1!=null ? propValue1 : System.getProperty(propName2, defaultValue);
    }

    // Create configuration based on system properties
    public static MongoConfiguration createConfiguration() {
        return new MongoConfiguration(
                getMongoHost(),
                getMongoPort(),
                getMongoDbName(),
                isClearCollectionsOnStartup()
        );
    }
}
