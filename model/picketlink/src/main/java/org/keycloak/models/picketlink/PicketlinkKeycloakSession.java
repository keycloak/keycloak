package org.keycloak.models.picketlink;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.picketlink.mappings.RealmData;
import org.keycloak.models.picketlink.relationships.RealmAdminRelationship;
import org.keycloak.models.utils.KeycloakSessionUtils;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.query.RelationshipQuery;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PicketlinkKeycloakSession implements KeycloakSession {
    public static ThreadLocal<EntityManager> currentEntityManager = new ThreadLocal<EntityManager>();
    public static ThreadLocal<Exception> setWhere = new ThreadLocal<Exception>();
    protected PartitionManager partitionManager;
    protected EntityManager entityManager;

    public PicketlinkKeycloakSession(PartitionManager partitionManager, EntityManager entityManager) {
        this.partitionManager = partitionManager;
        this.entityManager = entityManager;
        if (currentEntityManager.get() != null)
        {
            setWhere.get().printStackTrace();

            throw new IllegalStateException("Thread local was leaked!");
        }
        currentEntityManager.set(entityManager);
        setWhere.set(new Exception());
    }

    @Override
    public KeycloakTransaction getTransaction() {
        return new PicketlinkKeycloakTransaction(entityManager.getTransaction());
    }

    @Override
    public RealmAdapter createRealm(String name) {
        return createRealm(KeycloakSessionUtils.generateId(), name);
    }

    @Override
    public RealmAdapter createRealm(String id, String name) {
        // Picketlink beta 6 uses name attribute for getPartition()
        RealmData newRealm = new RealmData(id);
        newRealm.setId(id);
        newRealm.setRealmName(name);
        partitionManager.add(newRealm);
        RealmAdapter realm = new RealmAdapter(this, newRealm, partitionManager);
        return realm;
    }

    @Override
    public List<RealmModel> getRealms(UserModel admin) {
        RelationshipManager relationshipManager = partitionManager.createRelationshipManager();
        RelationshipQuery<RealmAdminRelationship> query = relationshipManager.createRelationshipQuery(RealmAdminRelationship.class);
        query.setParameter(RealmAdminRelationship.ADMIN, ((UserAdapter)admin).getUser());
        List<RealmAdminRelationship> results = query.getResultList();
        List<RealmModel> realmModels = new ArrayList<RealmModel>();
        for (RealmAdminRelationship relationship : results) {
            String realmName = relationship.getRealm();
            RealmModel model = getRealm(realmName);
            if (model == null) {
                relationshipManager.remove(relationship);
            } else {
                realmModels.add(model);
            }
        }
        return realmModels;
    }

    @Override
    public RealmAdapter getRealm(String id) {
        RealmData existing = partitionManager.getPartition(RealmData.class, id);
        if (existing == null) {
            return null;
        }
        return new RealmAdapter(this, existing, partitionManager);
    }

    @Override
    public void deleteRealm(RealmModel realm) {
        throw new RuntimeException("Not Implemented Yet");

    }

    @Override
    public void close() {
        setWhere.set(null);
        currentEntityManager.set(null);
        if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
        if (entityManager.isOpen()) entityManager.close();
    }
}
