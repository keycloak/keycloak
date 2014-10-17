package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UsernameLoginFailureAdapter implements UsernameLoginFailureModel {

    private InfinispanUserSessionProvider provider;
    private Cache<LoginFailureKey, LoginFailureEntity> cache;
    private LoginFailureKey key;
    private LoginFailureEntity entity;

    public UsernameLoginFailureAdapter(InfinispanUserSessionProvider provider, Cache<LoginFailureKey, LoginFailureEntity> cache, LoginFailureKey key, LoginFailureEntity entity) {
        this.provider = provider;
        this.cache = cache;
        this.key = key;
        this.entity = entity;
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    @Override
    public int getFailedLoginNotBefore() {
        return entity.getFailedLoginNotBefore();
    }

    @Override
    public void setFailedLoginNotBefore(int notBefore) {
        entity.setFailedLoginNotBefore(notBefore);
        update();
    }

    @Override
    public int getNumFailures() {
        return entity.getNumFailures();
    }

    @Override
    public void incrementFailures() {
        entity.setNumFailures(getNumFailures() + 1);
        update();
    }

    @Override
    public void clearFailures() {
        entity.setNumFailures(0);
        update();
    }

    @Override
    public long getLastFailure() {
        return entity.getLastFailure();
    }

    @Override
    public void setLastFailure(long lastFailure) {
        entity.setLastFailure(lastFailure);
        update();
    }

    @Override
    public String getLastIPFailure() {
        return entity.getLastIPFailure();
    }

    @Override
    public void setLastIPFailure(String ip) {
        entity.setLastIPFailure(ip);
        update();
    }

    void update() {
        provider.getTx().replace(cache, key, entity);
    }

}
