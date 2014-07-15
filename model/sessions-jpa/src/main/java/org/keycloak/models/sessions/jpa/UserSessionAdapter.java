package org.keycloak.models.sessions.jpa;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.jpa.entities.ClientUserSessionAssociationEntity;
import org.keycloak.models.sessions.jpa.entities.UserSessionEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
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
    public void setId(String id) {
        entity.setId(id);
    }

    @Override
    public UserModel getUser() {
        return session.users().getUserById(entity.getUserId(), realm);
    }

    @Override
    public void setUser(UserModel user) {
        entity.setUserId(user.getId());
    }

    @Override
    public String getIpAddress() {
        return entity.getIpAddress();
    }

    @Override
    public void setIpAddress(String ipAddress) {
        entity.setIpAddress(ipAddress);
    }

    @Override
    public int getStarted() {
        return entity.getStarted();
    }

    @Override
    public void setStarted(int started) {
        entity.setStarted(started);
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
    public void associateClient(ClientModel client) {
        for (ClientUserSessionAssociationEntity ass : entity.getClients()) {
            if (ass.getClientId().equals(client.getClientId())) return;
        }

        ClientUserSessionAssociationEntity association = new ClientUserSessionAssociationEntity();
        association.setClientId(client.getClientId());
        association.setSession(entity);
        em.persist(association);
        entity.getClients().add(association);
    }

    @Override
    public void removeAssociatedClient(ClientModel client) {
        em.createNamedQuery("removeClientUserSessionByClient").setParameter("clientId", client.getClientId()).executeUpdate();
    }

    @Override
    public List<ClientModel> getClientAssociations() {
        List<ClientModel> clients = new ArrayList<ClientModel>();
        for (ClientUserSessionAssociationEntity association : entity.getClients()) {
            clients.add(realm.findClient(association.getClientId()));
        }
        return clients;
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
