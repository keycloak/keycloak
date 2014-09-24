package org.keycloak.models.sessions.infinispan;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private final KeycloakSession session;

    private InfinispanUserSessionProvider provider;
    private final RealmModel realm;

    private final UserSessionEntity entity;

    public UserSessionAdapter(KeycloakSession session, InfinispanUserSessionProvider provider, RealmModel realm, UserSessionEntity entity) {
        this.session = session;
        this.provider = provider;
        this.realm = realm;
        this.entity = entity;
    }

    public String getId() {
        return entity.getId();
    }

    public void setId(String id) {
        throw new UnsupportedOperationException();
    }

    public UserModel getUser() {
        return session.users().getUserById(entity.getUser(), realm);
    }

    public void setUser(UserModel user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLoginUsername() {
        return entity.getLoginUsername();
    }

    @Override
    public void setLoginUsername(String loginUsername) {
        throw new UnsupportedOperationException();
    }

    public String getIpAddress() {
        return entity.getIpAddress();
    }

    public void setIpAddress(String ipAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public void setAuthMethod(String authMethod) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRememberMe() {
        return entity.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        entity.setRememberMe(rememberMe);
    }

    public int getStarted() {
        return entity.getStarted();
    }

    public void setStarted(int started) {
        throw new UnsupportedOperationException();
    }

    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        entity.setLastSessionRefresh(lastSessionRefresh);
        provider.update(entity);
    }

    @Override
    public List<ClientSessionModel> getClientSessions() {
        List<ClientSessionEntity> clientSessions = provider.clientSessions().eq("userSession", entity.getId()).list();
        return provider.wrapClientSessions(realm, clientSessions);
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
