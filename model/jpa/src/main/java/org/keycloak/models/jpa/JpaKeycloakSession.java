package org.keycloak.models.jpa;

import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.*;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaKeycloakSession implements KeycloakSession {
    protected EntityManager em;

    public JpaKeycloakSession(EntityManager em) {
        this.em = PersistenceExceptionConverter.create(em);
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return new JpaKeycloakTransaction(em);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmEntity realm = new RealmEntity();
        realm.setName(name);
        realm.setId(id);
        em.persist(realm);
        em.flush();
        return new RealmAdapter(em, realm);
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        return new RealmAdapter(em, realm);
    }

    @Override
    public List<RealmModel> getRealms() {
        TypedQuery<RealmEntity> query = em.createNamedQuery("getAllRealms", RealmEntity.class);
        List<RealmEntity> entities = query.getResultList();
        List<RealmModel> realms = new ArrayList<RealmModel>();
        for (RealmEntity entity : entities) {
            realms.add(new RealmAdapter(em, entity));
        }
        return realms;
    }

    @Override
    public RealmModel getRealmByName(String name) {
        TypedQuery<RealmEntity> query = em.createNamedQuery("getRealmByName", RealmEntity.class);
        query.setParameter("name", name);
        List<RealmEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        if (entities.size() > 1) throw new IllegalStateException("Should not be more than one realm with same name");
        RealmEntity realm = query.getResultList().get(0);
        if (realm == null) return null;
        return new RealmAdapter(em, realm);
    }

    @Override
    public UserModel getUserById(String id, String realmId) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserById", UserEntity.class);
        query.setParameter("id", id);
        RealmEntity realm = em.getReference(RealmEntity.class, realmId);
        query.setParameter("realm", realm);
        List<UserEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        return new UserAdapter(em, entities.get(0));
    }

    @Override
    public UserModel getUserByUsername(String username, String realmId) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByLoginName", UserEntity.class);
        query.setParameter("loginName", username);
        RealmEntity realm = em.getReference(RealmEntity.class, realmId);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return null;
        return new UserAdapter(em, results.get(0));
    }

    @Override
    public UserModel getUserByEmail(String email, String realmId) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByEmail", UserEntity.class);
        query.setParameter("email", email);
        RealmEntity realm = em.getReference(RealmEntity.class, realmId);
        query.setParameter("realm", realm);
        List<UserEntity> results = query.getResultList();
        return results.isEmpty() ? null : new UserAdapter(em, results.get(0));
    }

    @Override
    public boolean removeRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) {
            return false;
        }

        RealmAdapter adapter = new RealmAdapter(em, realm);
        adapter.removeUserSessions();
        for (ApplicationEntity a : new LinkedList<ApplicationEntity>(realm.getApplications())) {
            adapter.removeApplication(a.getId());
        }

        for (OAuthClientModel oauth : adapter.getOAuthClients()) {
            adapter.removeOAuthClient(oauth.getId());
        }

        for (UserEntity u : em.createQuery("from UserEntity u where u.realm = :realm", UserEntity.class).setParameter("realm", realm).getResultList()) {
            adapter.removeUser(u.getLoginName());
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
    public void removeAllData() {
        // Should be sufficient to delete all realms. Rest data should be removed in cascade
        List<RealmModel> realms = getRealms();
        for (RealmModel realm : realms) {
            removeRealm(realm.getId());
        }
    }
}
