package org.keycloak.connections.mongo;

import com.mongodb.DB;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface MongoConnectionProvider extends Provider {

    DB getDB();

    MongoStore getMongoStore();

    MongoStoreInvocationContext getInvocationContext();

}
