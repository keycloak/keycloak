package org.keycloak.models.mongo.keycloak.adapters;

import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.mongo.keycloak.entities.MongoUserSessionEntity;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private MongoUserSessionEntity entity;
    private RealmAdapter realm;
    private MongoStoreInvocationContext invContext;

    public UserSessionAdapter(MongoUserSessionEntity entity, RealmAdapter realm, MongoStoreInvocationContext invContext) {
        this.entity = entity;
        this.realm = realm;
        this.invContext = invContext;
    }

    public MongoUserSessionEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public void setId(String id) {
        entity.setId(id);
    }

    @Override
    public UserModel getUser() {
        return realm.getUserById(entity.getUser());
    }

    @Override
    public void setUser(UserModel user) {
        entity.setUser(user.getId());
    }

    @Override
    public String getIpAddress() {
        return entity.getIpAddress();
    }

    @Override
    public void setIpAddress(String ipAddress) {
        entity.setIpAddress(ipAddress);
    }

    @Override
    public int getStarted() {
        return entity.getStarted();
    }

    @Override
    public void setStarted(int started) {
        entity.setStarted(started);
    }

    @Override
    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        entity.setLastSessionRefresh(seconds);
    }

}
