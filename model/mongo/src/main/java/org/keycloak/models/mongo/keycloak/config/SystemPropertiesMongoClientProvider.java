package org.keycloak.models.mongo.keycloak.config;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.jboss.logging.Logger;

/**
 * Instance of {@link MongoClientProvider} which reads configuration of MongoDB from system properties
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SystemPropertiesMongoClientProvider implements MongoClientProvider {

    protected static final Logger logger = Logger.getLogger(SystemPropertiesMongoClientProvider.class);

    public static final String MONGO_HOST = "keycloak.mongo.host";
    public static final String MONGO_PORT = "keycloak.mongo.port";
    public static final String MONGO_DB_NAME = "keycloak.mongo.db";
    public static final String MONGO_CLEAR_ON_STARTUP = "keycloak.mongo.clearOnStartup";

    // Property names from Liveoak . Those are used as fallback
    private static final String MONGO_HOST_2 = "mongo.host";
    private static final String MONGO_PORT_2 = "mongo.port";
    private static final String MONGO_DB_NAME_2 = "mongo.db";
    private static final String MONGO_CLEAR_ON_STARTUP_2 = "mongo.clearOnStartup";

    // Port where MongoDB instance is normally started on linux. This port should be used if we're not starting embedded instance
    private static final String MONGO_DEFAULT_PORT = "27017";

    private MongoClient mongoClient;
    private DB db;

    @Override
    public synchronized MongoClient getMongoClient() {
        if (this.mongoClient == null) {
            init();
        }
        return this.mongoClient;
    }

    @Override
    public synchronized DB getDB() {
        if (mongoClient == null) {
            init();
        }
        return this.db;
    }

    @Override
    public boolean clearCollectionsOnStartup() {
        return isClearCollectionsOnStartup();
    }

    @Override
    public synchronized void close() {
        // Assume that client is dedicated just for Keycloak, so close it
        logger.info("Closing MongoDB client");
        mongoClient.close();
        mongoClient = null;
        db = null;
    }

    protected void init() {
        try {
            String host = getMongoHost();
            int port = getMongoPort();
            String dbName = getMongoDbName();
            boolean clearOnStartup = isClearCollectionsOnStartup();

            logger.info(String.format("Configuring MongoStore with: host=%s, port=%d, dbName=%s, clearOnStartup=%b", host, port, dbName, clearOnStartup));

            this.mongoClient = new MongoClient(host, port);
            this.db = mongoClient.getDB(dbName);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

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
}
