package org.keycloak.connections.mongo.updater;

import com.mongodb.DB;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface MongoUpdaterProvider extends Provider {

    public void update(KeycloakSession session, DB db);

}
