package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.mapreduce.ClientSessionMapper;
import org.keycloak.models.sessions.infinispan.mapreduce.FirstResultReducer;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private final KeycloakSession session;

    private final InfinispanUserSessionProvider provider;

    private final Cache<String, SessionEntity> cache;

    private final RealmModel realm;

    private final UserSessionEntity entity;

    public UserSessionAdapter(KeycloakSession session, InfinispanUserSessionProvider provider, Cache<String, SessionEntity> cache, RealmModel realm, UserSessionEntity entity) {
        this.session = session;
        this.provider = provider;
        this.cache = cache;
        this.realm = realm;
        this.entity = entity;
    }

    public String getId() {
        return entity.getId();
    }

    public void setId(String id) {
        throw new UnsupportedOperationException();
    }

    public UserModel getUser() {
        return session.users().getUserById(entity.getUser(), realm);
    }

    public void setUser(UserModel user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLoginUsername() {
        return entity.getLoginUsername();
    }

    @Override
    public void setLoginUsername(String loginUsername) {
        throw new UnsupportedOperationException();
    }

    public String getIpAddress() {
        return entity.getIpAddress();
    }

    public void setIpAddress(String ipAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public void setAuthMethod(String authMethod) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRememberMe() {
        return entity.isRememberMe();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        entity.setRememberMe(rememberMe);
    }

    public int getStarted() {
        return entity.getStarted();
    }

    public void setStarted(int started) {
        throw new UnsupportedOperationException();
    }

    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    public void setLastSessionRefresh(int lastSessionRefresh) {
        entity.setLastSessionRefresh(lastSessionRefresh);
        update();
    }

    @Override
    public List<ClientSessionModel> getClientSessions() {
        Map<String, ClientSessionEntity> map = new MapReduceTask(cache)
                .mappedWith(ClientSessionMapper.create(realm.getId()).userSession(entity.getId()))
                .reducedWith(new FirstResultReducer())
                .execute();

        return provider.wrapClientSessions(realm, map.values());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UserSessionModel)) return false;

        UserSessionModel that = (UserSessionModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    void update() {
        cache.replace(entity.getId(), entity);
    }

}
