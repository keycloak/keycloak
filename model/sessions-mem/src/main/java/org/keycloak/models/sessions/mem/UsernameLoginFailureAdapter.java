package org.keycloak.models.sessions.mem;

import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.mem.entities.UsernameLoginFailureEntity;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UsernameLoginFailureAdapter implements UsernameLoginFailureModel {

    private final UsernameLoginFailureEntity entity;

    public UsernameLoginFailureAdapter(UsernameLoginFailureEntity entity) {
        this.entity = entity;
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    public String getRealm() {
        return entity.getRealm();
    }

    @Override
    public int getFailedLoginNotBefore() {
        return entity.getFailedLoginNotBefore().get();
    }

    @Override
    public void setFailedLoginNotBefore(int notBefore) {
        entity.getFailedLoginNotBefore().set(notBefore);
    }

    @Override
    public int getNumFailures() {
        return entity.getNumFailures().get();
    }

    @Override
    public void incrementFailures() {
        entity.getNumFailures().incrementAndGet();
    }

    @Override
    public void clearFailures() {
        entity.getNumFailures().set(0);
    }

    @Override
    public long getLastFailure() {
        return entity.getLastFailure().get();
    }

    @Override
    public void setLastFailure(long lastFailure) {
        entity.getLastFailure().set(lastFailure);
    }

    @Override
    public String getLastIPFailure() {
        return entity.getLastIpFailure().get();
    }

    @Override
    public void setLastIPFailure(String ip) {
        entity.getLastIpFailure().set(ip);
    }

}
