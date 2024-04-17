package org.keycloak.models.sessions.infinispan.remote;

import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RemoteInfinispanSingleUseObjectProvider implements SingleUseObjectProvider {

    private final static Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final RemoteInfinispanKeycloakTransaction<String, SingleUseObjectValueEntity> transaction;

    public RemoteInfinispanSingleUseObjectProvider(KeycloakSession session, RemoteCache<String, SingleUseObjectValueEntity> cache) {
        transaction = new RemoteInfinispanKeycloakTransaction<>(cache);
        session.getTransactionManager().enlistAfterCompletion(transaction);
    }

    @Override
    public void put(String key, long lifespanSeconds, Map<String, String> notes) {
        transaction.put(key, wrap(notes), lifespanSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Map<String, String> get(String key) {
        return unwrap(transaction.get(key));
    }

    @Override
    public Map<String, String> remove(String key) {
        try {
            return unwrap(withReturnValue().remove(key));
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            logger.debugf(re, "Failed when removing code %s", key);
            return null;
        }
    }

    @Override
    public boolean replace(String key, Map<String, String> notes) {
        return withReturnValue().replace(key, wrap(notes)) != null;
    }

    @Override
    public boolean putIfAbsent(String key, long lifespanInSeconds) {
        try {
            return withReturnValue().putIfAbsent(key, wrap(null), lifespanInSeconds, TimeUnit.SECONDS) == null;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to use the token from different place.
            logger.debugf(re, "Failed when adding token %s", key);
            return false;
        }
    }

    @Override
    public boolean contains(String key) {
        return transaction.getCache().containsKey(key);
    }

    @Override
    public void close() {

    }

    private RemoteCache<String, SingleUseObjectValueEntity> withReturnValue() {
        return transaction.getCache().withFlags(Flag.FORCE_RETURN_VALUE);
    }

    private static Map<String, String> unwrap(SingleUseObjectValueEntity entity) {
        return entity == null ? null : entity.getNotes();
    }

    private static SingleUseObjectValueEntity wrap(Map<String, String> notes) {
        return new SingleUseObjectValueEntity(notes);
    }
}
