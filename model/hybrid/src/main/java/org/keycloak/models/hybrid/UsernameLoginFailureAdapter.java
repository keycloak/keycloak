package org.keycloak.models.hybrid;

import org.keycloak.models.UsernameLoginFailureModel;
import org.keycloak.models.sessions.LoginFailure;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UsernameLoginFailureAdapter implements UsernameLoginFailureModel {

    private HybridModelProvider provider;

    private LoginFailure loginFailure;

    UsernameLoginFailureAdapter(HybridModelProvider provider, LoginFailure loginFailure) {
        this.provider = provider;
        this.loginFailure = loginFailure;
    }

    @Override
    public String getUsername() {
        return loginFailure.getUsername();
    }

    @Override
    public int getFailedLoginNotBefore() {
        return loginFailure.getFailedLoginNotBefore();
    }

    @Override
    public void setFailedLoginNotBefore(int notBefore) {
        loginFailure.setFailedLoginNotBefore(notBefore);
    }

    @Override
    public int getNumFailures() {
        return loginFailure.getNumFailures();
    }

    @Override
    public void incrementFailures() {
        loginFailure.incrementFailures();
    }

    @Override
    public void clearFailures() {
        loginFailure.clearFailures();
    }

    @Override
    public long getLastFailure() {
        return loginFailure.getLastFailure();
    }

    @Override
    public void setLastFailure(long lastFailure) {
        loginFailure.setLastFailure(lastFailure);
    }

    @Override
    public String getLastIPFailure() {
        return loginFailure.getLastIPFailure();
    }

    @Override
    public void setLastIPFailure(String ip) {
        loginFailure.setLastIPFailure(ip);
    }

}
