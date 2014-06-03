package org.keycloak.models.jpa;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.ClientUserSessionAssociationEntity;
import org.keycloak.models.jpa.entities.UserSessionEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private RealmModel realm;
    private UserSessionEntity entity;
    private EntityManager em;

    public UserSessionAdapter(EntityManager em, RealmModel realm, UserSessionEntity entity) {
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
    public UserModel getUser() {
        return realm.getUserById(entity.getUserId());
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
            if (ass.getClientId().equals(client.getId())) return;
        }
        ClientUserSessionAssociationEntity association = new ClientUserSessionAssociationEntity();
        association.setClientId(client.getId());
        association.setSession(entity);
        association.setUserId(entity.getUserId());
        association.setRealmId(realm.getId());
        em.persist(association);
        entity.getClients().add(association);
    }

    @Override
    public void removeAssociatedClient(ClientModel client) {
        em.createNamedQuery("removeClientUserSessionByClient").setParameter("clientId", client.getId()).executeUpdate();

    }

    @Override
    public List<ClientModel> getClientAssociations() {
        List<ClientModel> clients = new ArrayList<ClientModel>();
        for (ClientUserSessionAssociationEntity association : entity.getClients()) {
            ClientModel client = realm.findClientById(association.getClientId());
            if (client == null) {
                throw new ModelException("couldnt find client");
            }
            clients.add(client);
        }
        return clients;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserSessionAdapter that = (UserSessionAdapter) o;
        return that.getId().equals(this.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
