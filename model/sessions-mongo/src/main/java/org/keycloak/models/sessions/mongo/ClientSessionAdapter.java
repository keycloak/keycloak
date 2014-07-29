package org.keycloak.models.sessions.mongo;

import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.mongo.entities.MongoClientSessionEntity;
import org.keycloak.models.sessions.mongo.entities.MongoUserSessionEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientSessionAdapter implements ClientSessionModel {

    private KeycloakSession session;
    private MongoUserSessionProvider provider;
    private RealmModel realm;
    private MongoClientSessionEntity entity;
    private MongoUserSessionEntity userSessionEntity;
    private MongoStoreInvocationContext invContext;

    public ClientSessionAdapter(KeycloakSession session, MongoUserSessionProvider provider, RealmModel realm, MongoClientSessionEntity entity, MongoUserSessionEntity userSessionEntity, MongoStoreInvocationContext invContext) {
        this.session = session;
        this.provider = provider;
        this.realm = realm;
        this.entity = entity;
        this.userSessionEntity = userSessionEntity;
        this.invContext = invContext;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public ClientModel getClient() {
        return realm.findClientById(entity.getClientId());
    }

    @Override
    public String getState() {
        return entity.getState();
    }

    @Override
    public UserSessionModel getUserSession() {
        return new UserSessionAdapter(session, provider, userSessionEntity, realm, invContext);
    }

    @Override
    public String getRedirectUri() {
        return entity.getRedirectUri();
    }

    @Override
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(timestamp);
        invContext.getMongoStore().updateEntity(userSessionEntity, invContext);
    }

    @Override
    public Action getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(Action action) {
        entity.setAction(action);
        invContext.getMongoStore().updateEntity(userSessionEntity, invContext);
    }

    @Override
    public Set<String> getRoles() {
        return entity.getRoles() != null ? new HashSet<String>(entity.getRoles()) : null;
    }

}
