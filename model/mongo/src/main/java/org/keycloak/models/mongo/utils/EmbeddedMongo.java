package org.keycloak.models.mongo.utils;

import java.io.IOException;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EmbeddedMongo {

    protected static final Logger logger = Logger.getLogger(EmbeddedMongo.class);

    private MongodExecutable mongodExe;
    private MongodProcess mongod;

    public void startEmbeddedMongo(int port) {
        logger.info("Going to start embedded Mongo on port=" + port);

        try {
            IMongodConfig mongodConfig = new MongodConfigBuilder()
                    .version(Version.Main.PRODUCTION)
                    .net(new Net(port, Network.localhostIsIPv6()))
                    .build();
            mongodExe = MongodStarter.getDefaultInstance().prepare(mongodConfig);
            mongod = mongodExe.start();
        } catch (IOException e) {
            logger.warn("Couldn't start Embedded Mongo on port " + port + ". Maybe it's already started? Cause: " + e.getClass() + " " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void stopEmbeddedMongo() {
        if (mongodExe != null) {
            if (mongod != null) {
                logger.info("Going to stop embedded MongoDB.");
                mongod.stop();
            }
            mongodExe.stop();
        }
    }
}
