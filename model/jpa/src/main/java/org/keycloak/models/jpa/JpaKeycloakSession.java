package org.keycloak.models.jpa;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.utils.KeycloakSessionUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaKeycloakSession implements KeycloakSession {
    protected EntityManager em;

    public JpaKeycloakSession(EntityManager em) {
        this.em = em;
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return new JpaKeycloakTransaction(em);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakSessionUtils.generateId(), name);
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
    public List<RealmModel> getRealms(UserModel admin) {
        TypedQuery<RealmEntity> query = em.createQuery("select r from RealmEntity r", RealmEntity.class);
        List<RealmEntity> entities = query.getResultList();
        List<RealmModel> realms = new ArrayList<RealmModel>();
        for (RealmEntity entity : entities) {
            realms.add(new RealmAdapter(em, entity));
        }
        return realms;
    }

    @Override
    public void deleteRealm(RealmModel realm) {
        throw new RuntimeException("Not Implemented Yet");
    }

    @Override
    public void close() {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em.isOpen()) em.close();
    }
}
