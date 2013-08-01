package org.keycloak.services.models.picketlink;

import org.jboss.resteasy.spi.NotImplementedYetException;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakTransaction;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.picketlink.mappings.RealmData;
import org.picketlink.idm.PartitionManager;

import javax.persistence.EntityManager;
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
        if (entityManager.getTransaction().isActive()) entityManager.getTransaction().rollback();
        setWhere.set(null);
        currentEntityManager.set(null);
        if (entityManager.isOpen()) entityManager.close();
    }
}
