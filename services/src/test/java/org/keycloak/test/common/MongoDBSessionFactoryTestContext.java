package org.keycloak.test.common;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.utils.PropertiesManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoDBSessionFactoryTestContext implements SessionFactoryTestContext {

    protected static final Logger logger = Logger.getLogger(MongoDBSessionFactoryTestContext.class);
    private static final int PORT = PropertiesManager.MONGO_DEFAULT_PORT_UNIT_TESTS;

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

        // Reset this, so other tests are not affected
        PropertiesManager.setDefaultSessionFactoryType();
    }

    @Override
    public void initEnvironment() {
        PropertiesManager.setSessionFactoryType(PropertiesManager.SESSION_FACTORY_MONGO);
        PropertiesManager.setMongoHost("localhost");
        PropertiesManager.setMongoPort(PORT);
        PropertiesManager.setMongoDbName("keycloakTest");
        PropertiesManager.setDropDatabaseOnStartup(true);
    }
}
