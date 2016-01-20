package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.ClientInitialAccessEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientInitialAccessAdapter implements ClientInitialAccessModel {

    private final KeycloakSession session;
    private final InfinispanUserSessionProvider provider;
    private final Cache<String, SessionEntity> cache;
    private final RealmModel realm;
    private final ClientInitialAccessEntity entity;

    public ClientInitialAccessAdapter(KeycloakSession session, InfinispanUserSessionProvider provider, Cache<String, SessionEntity> cache, RealmModel realm, ClientInitialAccessEntity entity) {
        this.session = session;
        this.provider = provider;
        this.cache = cache;
        this.realm = realm;
        this.entity = entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public int getExpiration() {
        return entity.getExpiration();
    }

    @Override
    public int getCount() {
        return entity.getCount();
    }

    @Override
    public int getRemainingCount() {
        return entity.getRemainingCount();
    }

    @Override
    public void decreaseRemainingCount() {
        entity.setRemainingCount(entity.getRemainingCount() - 1);
        update();
    }

    void update() {
        provider.getTx().replace(cache, entity.getId(), entity);
    }

}
