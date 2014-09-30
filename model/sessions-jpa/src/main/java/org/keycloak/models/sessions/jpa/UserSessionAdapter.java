package org.keycloak.models.sessions.jpa;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.jpa.entities.ClientSessionEntity;
import org.keycloak.models.sessions.jpa.entities.UserSessionEntity;

import javax.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private KeycloakSession session;
    private RealmModel realm;
    private UserSessionEntity entity;
    private EntityManager em;

    public UserSessionAdapter(KeycloakSession session, EntityManager em, RealmModel realm, UserSessionEntity entity) {
        this.session = session;
        this.realm = realm;
        this.entity = entity;
        this.em = em;
    }

    public UserSessionEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public UserModel getUser() {
        return session.users().getUserById(entity.getUserId(), realm);
    }

    @Override
    public String getLoginUsername() {
        return entity.getLoginUsername();
    }

    @Override
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

    @Override
    public int getStarted() {
        return entity.getStarted();
    }

    @Override
    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        entity.setLastSessionRefresh(seconds);
    }

    @Override
    public List<ClientSessionModel> getClientSessions() {
        List<ClientSessionModel> clientSessions = new LinkedList<ClientSessionModel>();
        for (ClientSessionEntity e : entity.getClientSessions()) {
            clientSessions.add(new ClientSessionAdapter(session, em, realm, e));
        }
        return clientSessions;
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
