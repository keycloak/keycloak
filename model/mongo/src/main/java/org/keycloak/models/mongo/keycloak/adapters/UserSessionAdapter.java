package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.jboss.logging.Logger;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.entities.ClientEntity;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.MongoApplicationEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoOAuthClientEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserSessionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter extends AbstractMongoAdapter<MongoUserSessionEntity> implements UserSessionModel {

    private static final Logger logger = Logger.getLogger(RealmAdapter.class);

    private MongoUserSessionEntity entity;
    private RealmModel realm;

    public UserSessionAdapter(MongoUserSessionEntity entity, RealmModel realm, MongoStoreInvocationContext invContext)
    {
        super(invContext);
        this.entity = entity;
        this.realm = realm;
    }

    @Override
    protected MongoUserSessionEntity getMongoEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public void setId(String id) {
        entity.setId(id);
        updateMongoEntity();
    }

    @Override
    public UserModel getUser() {
        return realm.getUserById(entity.getUser());
    }

    @Override
    public void setUser(UserModel user) {
        entity.setUser(user.getId());
        updateMongoEntity();
    }

    @Override
    public String getIpAddress() {
        return entity.getIpAddress();
    }

    @Override
    public void setIpAddress(String ipAddress) {
        entity.setIpAddress(ipAddress);
        updateMongoEntity();
    }

    @Override
    public int getStarted() {
        return entity.getStarted();
    }

    @Override
    public void setStarted(int started) {
        entity.setStarted(started);
        updateMongoEntity();
    }

    @Override
    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        entity.setLastSessionRefresh(seconds);
        updateMongoEntity();
    }

    @Override
    public void associateClient(ClientModel client) {
        getMongoStore().pushItemToList(entity, "associatedClientIds", client.getId(), true, invocationContext);
    }

    @Override
    public List<ClientModel> getClientAssociations() {
        List<String> associatedClientIds = getMongoEntity().getAssociatedClientIds();

        List<ClientModel> clients = new ArrayList<ClientModel>();
        for (String clientId : associatedClientIds) {
            // Try application first
            ClientModel client = realm.getApplicationById(clientId);

            // And then OAuthClient
            if (client == null) {
                client = realm.getOAuthClientById(clientId);
            }

            if (client != null) {
                clients.add(client);
            } else {
                logger.warnf("Not found associated client with Id: %s", clientId);
            }
        }
        return clients;
    }

    @Override
    public void removeAssociatedClient(ClientModel client) {
        getMongoStore().pullItemFromList(entity, "associatedClientIds", client.getId(), invocationContext);
    }
}
