package org.keycloak.models.sessions.mem.entities;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UsernameLoginFailureEntity {

    private String username;
    private String realm;

    private AtomicInteger failedLoginNotBefore = new AtomicInteger();
    private AtomicInteger numFailures = new AtomicInteger();
    private AtomicLong lastFailure = new AtomicLong();
    private AtomicReference<String> lastIpFailure = new AtomicReference<String>();

    public UsernameLoginFailureEntity(String username, String realm) {
        this.username = username;
        this.realm = realm;
    }

    public String getUsername() {
        return username;
    }

    public String getRealm() {
        return realm;
    }

    public AtomicInteger getFailedLoginNotBefore() {
        return failedLoginNotBefore;
    }

    public void setFailedLoginNotBefore(AtomicInteger failedLoginNotBefore) {
        this.failedLoginNotBefore = failedLoginNotBefore;
    }

    public AtomicInteger getNumFailures() {
        return numFailures;
    }

    public void setNumFailures(AtomicInteger numFailures) {
        this.numFailures = numFailures;
    }

    public AtomicLong getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(AtomicLong lastFailure) {
        this.lastFailure = lastFailure;
    }

    public AtomicReference<String> getLastIpFailure() {
        return lastIpFailure;
    }

    public void setLastIpFailure(AtomicReference<String> lastIpFailure) {
        this.lastIpFailure = lastIpFailure;
    }

}
