package org.keycloak.models.jpa;

import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserSessionEntity;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionAdapter implements UserSessionModel {

    private UserSessionEntity entity;

    public UserSessionAdapter(UserSessionEntity entity) {
        this.entity = entity;
    }

    public UserSessionEntity getEntity() {
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
        return new UserAdapter(entity.getUser());
    }

    @Override
    public void setUser(UserModel user) {
        entity.setUser(((UserAdapter) user).getUser());
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
    public int getExpires() {
        return entity.getExpires();
    }

    @Override
    public void setExpires(int expires) {
        entity.setExpires(expires);
    }

}
