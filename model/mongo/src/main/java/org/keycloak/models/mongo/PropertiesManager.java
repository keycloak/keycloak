package org.keycloak.models.mongo;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PropertiesManager {

    private static final String MONGO_HOST = "keycloak.mongodb.host";
    private static final String MONGO_PORT = "keycloak.mongodb.port";
    private static final String MONGO_DB_NAME = "keycloak.mongodb.databaseName";
    private static final String MONGO_DROP_DB_ON_STARTUP = "keycloak.mongodb.dropDatabaseOnStartup";
    private static final String BOOTSTRAP_EMBEDDED_MONGO_AT_CONTEXT_INIT = "keycloak.mongodb.bootstrapEmbeddedMongoAtContextInit";

    // Port where embedded MongoDB will be started during keycloak bootstrap. Same port will be used by KeycloakApplication then
    private static final int MONGO_DEFAULT_PORT_KEYCLOAK_WAR_EMBEDDED = 37017;

    // Port where MongoDB instance is normally started on linux. This port should be used if we're not starting embedded instance (keycloak.mongodb.bootstrapEmbeddedMongoAtContextInit is false)
    private static final int MONGO_DEFAULT_PORT_KEYCLOAK_WAR = 27017;

    // Port where unit tests will start embedded MongoDB instance
    public static final int MONGO_DEFAULT_PORT_UNIT_TESTS = 27777;

    public static String getMongoHost() {
        return System.getProperty(MONGO_HOST, "localhost");
    }

    public static void setMongoHost(String mongoHost) {
        System.setProperty(MONGO_HOST, mongoHost);
    }

    public static int getMongoPort() {
        return Integer.parseInt(System.getProperty(MONGO_PORT, String.valueOf(MONGO_DEFAULT_PORT_KEYCLOAK_WAR_EMBEDDED)));
    }

    public static void setMongoPort(int mongoPort) {
        System.setProperty(MONGO_PORT, String.valueOf(mongoPort));
    }

    public static String getMongoDbName() {
        return System.getProperty(MONGO_DB_NAME, "keycloak");
    }

    public static void setMongoDbName(String mongoMongoDbName) {
        System.setProperty(MONGO_DB_NAME, mongoMongoDbName);
    }

    public static boolean dropDatabaseOnStartup() {
        return Boolean.parseBoolean(System.getProperty(MONGO_DROP_DB_ON_STARTUP, "true"));
    }

    public static void setDropDatabaseOnStartup(boolean dropDatabaseOnStartup) {
        System.setProperty(MONGO_DROP_DB_ON_STARTUP, String.valueOf(dropDatabaseOnStartup));
    }

    public static boolean bootstrapEmbeddedMongoAtContextInit() {
        return isMongoSessionFactory() && Boolean.parseBoolean(System.getProperty(BOOTSTRAP_EMBEDDED_MONGO_AT_CONTEXT_INIT, "true"));
    }
}
