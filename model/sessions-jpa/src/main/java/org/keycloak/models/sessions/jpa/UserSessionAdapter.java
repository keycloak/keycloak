package org.keycloak.models.sessions.jpa;

import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.Session;
import org.keycloak.models.sessions.jpa.entities.ClientUserSessionAssociationEntity;
import org.keycloak.models.sessions.jpa.entities.UserSessionEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements Session {

    private String realm;
    private UserSessionEntity entity;
    private EntityManager em;

    public UserSessionAdapter(EntityManager em, String realm, UserSessionEntity entity) {
        this.entity = entity;
        this.em = em;
        this.realm = realm;
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
    public String getUser() {
        return entity.getUserId();
    }

    @Override
    public void setUser(String user) {
        entity.setUserId(user);
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
    public void associateClient(String client) {
        for (ClientUserSessionAssociationEntity ass : entity.getClients()) {
            if (ass.getClientId().equals(client)) return;
        }
        ClientUserSessionAssociationEntity association = new ClientUserSessionAssociationEntity();
        association.setClientId(client);
        association.setSession(entity);
        association.setUserId(entity.getUserId());
        association.setRealmId(realm);
        em.persist(association);
        entity.getClients().add(association);
    }

    @Override
    public void removeAssociatedClient(String client) {
        em.createNamedQuery("removeClientUserSessionByClient").setParameter("clientId", client).executeUpdate();
    }

    @Override
    public List<String> getClientAssociations() {
        List<String> clients = new ArrayList<String>();
        for (ClientUserSessionAssociationEntity association : entity.getClients()) {
            clients.add(association.getClientId());
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
