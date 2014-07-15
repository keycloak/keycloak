package org.keycloak.models.realms.jpa;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.realms.Application;
import org.keycloak.models.realms.OAuthClient;
import org.keycloak.models.realms.Realm;
import org.keycloak.models.realms.RealmProvider;
import org.keycloak.models.realms.Role;
import org.keycloak.models.realms.jpa.entities.ApplicationEntity;
import org.keycloak.models.realms.jpa.entities.OAuthClientEntity;
import org.keycloak.models.realms.jpa.entities.RealmEntity;
import org.keycloak.models.realms.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaRealmProvider implements RealmProvider {

    protected final EntityManager em;

    public JpaRealmProvider(EntityManager em) {
        this.em = PersistenceExceptionConverter.create(em);
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return new JpaKeycloakTransaction(em);
    }

    @Override
    public Realm createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public Realm createRealm(String id, String name) {
        RealmEntity realm = new RealmEntity();
        realm.setName(name);
        realm.setId(id);
        em.persist(realm);
        em.flush();
        return new RealmAdapter(this, em, realm);
    }

    @Override
    public Realm getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        return new RealmAdapter(this, em, realm);
    }

    @Override
    public List<Realm> getRealms() {
        TypedQuery<RealmEntity> query = em.createNamedQuery("getAllRealms", RealmEntity.class);
        List<RealmEntity> entities = query.getResultList();
        List<Realm> realms = new ArrayList<Realm>();
        for (RealmEntity entity : entities) {
            realms.add(new RealmAdapter(this, em, entity));
        }
        return realms;
    }

    @Override
    public Realm getRealmByName(String name) {
        TypedQuery<RealmEntity> query = em.createNamedQuery("getRealmByName", RealmEntity.class);
        query.setParameter("name", name);
        List<RealmEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        if (entities.size() > 1) throw new IllegalStateException("Should not be more than one realm with same name");
        RealmEntity realm = query.getResultList().get(0);
        if (realm == null) return null;
        return new RealmAdapter(this, em, realm);
    }

    @Override
    public boolean removeRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) {
            return false;
        }

        RealmAdapter adapter = new RealmAdapter(this, em, realm);
        for (Application a : adapter.getApplications()) {
            adapter.removeApplication(a);
        }

        for (OAuthClient oauth : adapter.getOAuthClients()) {
            adapter.removeOAuthClient(oauth);
        }

        em.remove(realm);

        return true;
    }

    @Override
    public void close() {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em.isOpen()) em.close();
    }

    @Override
    public Role getRoleById(String id, String realm) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        if (!realm.equals(entity.getRealmId())) return null;
        return new RoleAdapter(this, em, entity);
    }

    @Override
    public Application getApplicationById(String id, String realm) {
        ApplicationEntity app = em.find(ApplicationEntity.class, id);

        // Check if application belongs to this realm
        if (app == null || !realm.equals(app.getRealm().getId())) return null;
        return new ApplicationAdapter(this, em, app);
    }

    @Override
    public OAuthClient getOAuthClientById(String id, String realm) {
        OAuthClientEntity client = em.find(OAuthClientEntity.class, id);

        // Check if client belongs to this realm
        if (client == null || !realm.equals(client.getRealm().getId())) return null;
        return new OAuthClientAdapter(this, client, em);
    }

}
