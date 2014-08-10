package org.keycloak.models.jpa;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ApplicationEntity;
import org.keycloak.models.jpa.entities.OAuthClientEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaRealmProvider implements RealmProvider {
    private final KeycloakSession session;
    protected EntityManager em;

    public JpaRealmProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
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
        return new RealmAdapter(session, em, realm);
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        return new RealmAdapter(session, em, realm);
    }

    @Override
    public List<RealmModel> getRealms() {
        TypedQuery<RealmEntity> query = em.createNamedQuery("getAllRealms", RealmEntity.class);
        List<RealmEntity> entities = query.getResultList();
        List<RealmModel> realms = new ArrayList<RealmModel>();
        for (RealmEntity entity : entities) {
            realms.add(new RealmAdapter(session, em, entity));
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
        return new RealmAdapter(session, em, realm);
    }

    @Override
    public boolean removeRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) {
            return false;
        }

        RealmAdapter adapter = new RealmAdapter(session, em, realm);
        session.users().preRemove(adapter);
        for (ApplicationEntity a : new LinkedList<ApplicationEntity>(realm.getApplications())) {
            adapter.removeApplication(a.getId());
        }

        for (OAuthClientModel oauth : adapter.getOAuthClients()) {
            adapter.removeOAuthClient(oauth.getId());
        }

       em.remove(realm);
        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public RoleModel getRoleById(String id, RealmModel realm) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        if (!realm.getId().equals(entity.getRealmId())) return null;
        return new RoleAdapter(realm, em, entity);
    }

    @Override
    public ApplicationModel getApplicationById(String id, RealmModel realm) {
        ApplicationEntity app = em.find(ApplicationEntity.class, id);

        // Check if application belongs to this realm
        if (app == null || !realm.getId().equals(app.getRealm().getId())) return null;
        return new ApplicationAdapter(realm, em, session, app);
    }

    @Override
    public OAuthClientModel getOAuthClientById(String id, RealmModel realm) {
        OAuthClientEntity client = em.find(OAuthClientEntity.class, id);

        // Check if client belongs to this realm
        if (client == null || !realm.getId().equals(client.getRealm().getId())) return null;
        return new OAuthClientAdapter(realm, client, em);
    }

}
