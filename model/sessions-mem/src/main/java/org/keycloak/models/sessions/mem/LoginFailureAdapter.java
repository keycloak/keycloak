package org.keycloak.models.sessions.mem;

import org.keycloak.models.sessions.LoginFailure;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginFailureAdapter implements LoginFailure {

    private final String username;
    private final String realm;

    private AtomicInteger failedLoginNotBefore = new AtomicInteger();
    private AtomicInteger numFailures = new AtomicInteger();
    private AtomicLong lastFailure = new AtomicLong();
    private AtomicReference<String> lastIpFailure = new AtomicReference<String>();

    public LoginFailureAdapter(String username, String realm) {
        this.username = username;
        this.realm = realm;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getRealm() {
        return realm;
    }

    @Override
    public int getFailedLoginNotBefore() {
        return failedLoginNotBefore.get();
    }

    @Override
    public void setFailedLoginNotBefore(int notBefore) {
        failedLoginNotBefore.set(notBefore);
    }

    @Override
    public int getNumFailures() {
        return numFailures.get();
    }

    @Override
    public void incrementFailures() {
         numFailures.incrementAndGet();
    }

    @Override
    public void clearFailures() {
        numFailures.set(0);
    }

    @Override
    public long getLastFailure() {
        return lastFailure.get();
    }

    @Override
    public void setLastFailure(long lastFailure) {
        this.lastFailure.set(lastFailure);
    }

    @Override
    public String getLastIPFailure() {
        return lastIpFailure.get();
    }

    @Override
    public void setLastIPFailure(String ip) {
        lastIpFailure.set(ip);
    }

}
