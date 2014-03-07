package org.keycloak.models.mongo.keycloak.config;

import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface MongoClientProvider {

    MongoClient getMongoClient();

    DB getDB();

    /**
     * @return true if collections should be cleared on startup
     */
    boolean clearCollectionsOnStartup();

    void close();
}
