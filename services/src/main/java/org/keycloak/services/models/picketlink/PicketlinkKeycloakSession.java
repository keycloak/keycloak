package org.keycloak.services.models.picketlink;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.NotImplementedYetException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakTransaction;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.picketlink.mappings.RealmData;
import org.keycloak.services.models.picketlink.relationships.RealmAdminRelationship;
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
    public static ThreadLocal<String> setFromPath = new ThreadLocal<String>();
    protected PartitionManager partitionManager;
    protected EntityManager entityManager;

    private static AtomicLong counter = new AtomicLong(1);
    public static String generateId() {
        return counter.getAndIncrement() + "-" + System.currentTimeMillis();
    }

    public PicketlinkKeycloakSession(PartitionManager partitionManager, EntityManager entityManager) {
        this.partitionManager = partitionManager;
        this.entityManager = entityManager;
        if (currentEntityManager.get() != null)
        {
            setWhere.get().printStackTrace();
            String path = setFromPath.get();
            if (path == null) path = "???";

            throw new IllegalStateException("Thread local was leaked! from path: " + path);
        }
        HttpRequest request = ResteasyProviderFactory.getContextData(HttpRequest.class);
        if (request != null) {
            setFromPath.set(request.getUri().getPath());
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
        return createRealm(generateId(), name);
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
        throw new NotImplementedYetException();

    }

    @Override
    public void close() {
        setFromPath.set(null);
        setWhere.set(null);
        currentEntityManager.set(null);
        if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
        if (entityManager.isOpen()) entityManager.close();
    }
}
