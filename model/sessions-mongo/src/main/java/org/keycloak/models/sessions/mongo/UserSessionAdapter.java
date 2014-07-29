package org.keycloak.models.sessions.mongo;

import org.jboss.logging.Logger;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.mongo.entities.MongoClientSessionEntity;
import org.keycloak.models.sessions.mongo.entities.MongoUserSessionEntity;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter extends AbstractMongoAdapter<MongoUserSessionEntity> implements UserSessionModel {

    private static final Logger logger = Logger.getLogger(UserSessionAdapter.class);

    private final MongoUserSessionProvider provider;
    private MongoUserSessionEntity entity;
    private RealmModel realm;
    private KeycloakSession keycloakSession;
    private final MongoStoreInvocationContext invContext;

    public UserSessionAdapter(KeycloakSession keycloakSession, MongoUserSessionProvider provider, MongoUserSessionEntity entity, RealmModel realm, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.provider = provider;
        this.entity = entity;
        this.realm = realm;
        this.keycloakSession = keycloakSession;
        this.invContext = invContext;
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
    public List<ClientSessionModel> getClientSessions() {
        List<ClientSessionModel> sessions = new LinkedList<ClientSessionModel>();
        for (MongoClientSessionEntity e : entity.getClientSessions()) {
            sessions.add(new ClientSessionAdapter(keycloakSession, provider, realm, e, entity, invocationContext));
        }
        return sessions;
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
