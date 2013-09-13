package org.keycloak.test.common;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.resources.KeycloakApplication;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBSessionFactoryTestContext implements SessionFactoryTestContext {

    protected static final Logger logger = Logger.getLogger(MongoDBSessionFactoryTestContext.class);
    private static final int PORT = 27777;

    private MongodExecutable mongodExe;
    private MongodProcess mongod;

    @Override
    public void beforeTestClass() {
        logger.info("Bootstrapping MongoDB on localhost, port " + PORT);
        try {
            mongodExe = MongodStarter.getDefaultInstance().prepare(new MongodConfig(Version.V2_0_5, PORT, Network.localhostIsIPv6()));
            mongod = mongodExe.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("MongoDB bootstrapped successfully");
    }

    @Override
    public void afterTestClass() {
        if (mongodExe != null) {
            if (mongod != null) {
                mongod.stop();
            }
            mongodExe.stop();
        }
        logger.info("MongoDB stopped successfully");

        // Null this, so other tests are not affected
        System.setProperty(KeycloakApplication.SESSION_FACTORY, "");
    }

    @Override
    public void initEnvironment() {
        System.setProperty(KeycloakApplication.SESSION_FACTORY, KeycloakApplication.SESSION_FACTORY_MONGO);
        System.setProperty(KeycloakApplication.MONGO_HOST, "localhost");
        System.setProperty(KeycloakApplication.MONGO_PORT, String.valueOf(PORT));
        System.setProperty(KeycloakApplication.MONGO_DB_NAME, "keycloakTest");
        System.setProperty(KeycloakApplication.MONGO_DROP_DB_ON_STARTUP, "true");
    }
}
