package org.keycloak.models.sessions.infinispan.compat;

import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.compat.entities.ClientInitialAccessEntity;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientInitialAccessAdapter implements ClientInitialAccessModel {

    private final RealmModel realm;
    private final ClientInitialAccessEntity entity;

    public ClientInitialAccessAdapter(RealmModel realm, ClientInitialAccessEntity entity) {
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
        return entity.getExpires();
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
    }

}
