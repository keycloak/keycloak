package org.keycloak.services.listeners;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.services.utils.PropertiesManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoRunnerListener implements ServletContextListener {

    protected static final Logger logger = Logger.getLogger(MongoRunnerListener.class);

    private MongodExecutable mongodExe;
    private MongodProcess mongod;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (PropertiesManager.bootstrapEmbeddedMongoAtContextInit()) {
            int port = PropertiesManager.getMongoPort();
            logger.info("Going to start embedded MongoDB on port=" + port);

            try {
                mongodExe = MongodStarter.getDefaultInstance().prepare(new MongodConfig(Version.V2_0_5, port, Network.localhostIsIPv6()));
                mongod = mongodExe.start();
            } catch (Exception e) {
                logger.warn("Couldn't start Embedded Mongo on port " + port + ". Maybe it's already started? Cause: " + e.getClass() + " " + e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to start MongoDB", e);
                }
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (mongodExe != null) {
            if (mongod != null) {
                logger.info("Going to stop embedded MongoDB.");
                mongod.stop();
            }
            mongodExe.stop();
        }
    }
}
