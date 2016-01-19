package org.keycloak.connections.mongo;

import com.mongodb.DB;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultMongoConnectionProvider implements MongoConnectionProvider {

    private DB db;
    private MongoStore mongoStore;
    private MongoStoreInvocationContext invocationContext;

    public DefaultMongoConnectionProvider(DB db, MongoStore mongoStore, MongoStoreInvocationContext invocationContext) {
        this.db = db;
        this.mongoStore = mongoStore;
        this.invocationContext = invocationContext;
    }

    @Override
    public DB getDB() {
        return db;
    }

    @Override
    public MongoStore getMongoStore() {
        return mongoStore;
    }

    @Override
    public MongoStoreInvocationContext getInvocationContext() {
        return invocationContext;
    }

    @Override
    public void close() {
    }

}
