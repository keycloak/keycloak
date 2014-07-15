package org.keycloak.models.sessions.mem;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.mem.entities.UserSessionEntity;

import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private final KeycloakSession session;

    private final RealmModel realm;

    private final UserSessionEntity entity;

    public UserSessionAdapter(KeycloakSession session, RealmModel realm, UserSessionEntity entity) {
        this.session = session;
        this.realm = realm;
        this.entity = entity;
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

    public String getIpAddress() {
        return entity.getIpAddress();
    }

    public void setIpAddress(String ipAddress) {
        entity.setIpAddress(ipAddress);
    }

    public int getStarted() {
        return entity.getStarted();
    }

    public void setStarted(int started) {
        entity.setStarted(started);
    }

    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        entity.setLastSessionRefresh(lastSessionRefresh);
    }

    @Override
    public void associateClient(ClientModel client) {
        if (!entity.getClients().contains(client.getClientId())) {
            entity.getClients().add(client.getClientId());
        }
    }

    @Override
    public List<ClientModel> getClientAssociations() {
        List<ClientModel> models = new LinkedList<ClientModel>();
        for (String clientId : entity.getClients()) {
            models.add(realm.findClient(clientId));
        }
        return models;
    }

    @Override
    public void removeAssociatedClient(ClientModel client) {
        entity.getClients().remove(client.getClientId());
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
