package org.keycloak.models.sessions.mongo;

import org.jboss.logging.Logger;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.mongo.entities.MongoUserSessionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter extends AbstractMongoAdapter<MongoUserSessionEntity> implements UserSessionModel {

    private static final Logger logger = Logger.getLogger(UserSessionAdapter.class);

    private MongoUserSessionEntity entity;
    private RealmModel realm;
    private KeycloakSession keycloakSession;

    public UserSessionAdapter(KeycloakSession keycloakSession, MongoUserSessionEntity entity, RealmModel realm, MongoStoreInvocationContext invContext)
    {
        super(invContext);
        this.entity = entity;
        this.realm = realm;
        this.keycloakSession = keycloakSession;
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
        return keycloakSession.users().getUserById(entity.getUser(), realm);
    }

    @Override
    public void setUser(UserModel user) {
        entity.setUser(user.getId());
        updateMongoEntity();
    }

    @Override
    public String getLoginUsername() {
        return entity.getLoginUsername();
    }

    @Override
    public void setLoginUsername(String loginUsername) {
        entity.setLoginUsername(loginUsername);
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
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public void setAuthMethod(String authMethod) {
        entity.setAuthMethod(authMethod);
        updateMongoEntity();
    }

    @Override
    public boolean isRememberMe() {
        return entity.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        entity.setRememberMe(rememberMe);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UserSessionModel)) return false;

        UserSessionModel that = (UserSessionModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
