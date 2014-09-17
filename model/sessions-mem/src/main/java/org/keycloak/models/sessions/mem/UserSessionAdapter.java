package org.keycloak.models.sessions.mem;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.mem.entities.ClientSessionEntity;
import org.keycloak.models.sessions.mem.entities.UserSessionEntity;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private final KeycloakSession session;

    private MemUserSessionProvider provider;
    private final RealmModel realm;

    private final UserSessionEntity entity;

    public UserSessionAdapter(KeycloakSession session, MemUserSessionProvider provider, RealmModel realm, UserSessionEntity entity) {
        this.session = session;
        this.provider = provider;
        this.realm = realm;
        this.entity = entity;
    }

    public UserSessionEntity getEntity() {
        return entity;
    }

    public String getId() {
        return entity.getId();
    }

    public void setId(String id) {
        entity.setId(id);
    }

    public UserModel getUser() {
        return session.users().getUserById(entity.getUser(), realm);
    }

    public void setUser(UserModel user) {
        entity.setUser(user.getId());
    }

    @Override
    public String getLoginUsername() {
        return entity.getLoginUsername();
    }

    public String getIpAddress() {
        return entity.getIpAddress();
    }

    @Override
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public boolean isRememberMe() {
        return entity.isRememberMe();
    }

    public int getStarted() {
        return entity.getStarted();
    }

    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        entity.setLastSessionRefresh(lastSessionRefresh);
    }

    @Override
    public List<ClientSessionModel> getClientSessions() {
        List<ClientSessionModel> clientSessionModels = new LinkedList<ClientSessionModel>();
        if (entity.getClientSessions() != null) {
            for (ClientSessionEntity e : entity.getClientSessions()) {
                clientSessionModels.add(new ClientSessionAdapter(session, provider, realm, e));
            }
        }
        return clientSessionModels;
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
