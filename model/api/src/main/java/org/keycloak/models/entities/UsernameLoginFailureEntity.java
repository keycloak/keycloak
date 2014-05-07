package org.keycloak.models.entities;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UsernameLoginFailureEntity extends AbstractIdentifiableEntity {

    private String username;
    private int failedLoginNotBefore;
    private int numFailures;
    private long lastFailure;
    private String lastIPFailure;

    private String realmId;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getFailedLoginNotBefore() {
        return failedLoginNotBefore;
    }

    public void setFailedLoginNotBefore(int failedLoginNotBefore) {
        this.failedLoginNotBefore = failedLoginNotBefore;
    }

    public int getNumFailures() {
        return numFailures;
    }

    public void setNumFailures(int numFailures) {
        this.numFailures = numFailures;
    }

    public long getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(long lastFailure) {
        this.lastFailure = lastFailure;
    }

    public String getLastIPFailure() {
        return lastIPFailure;
    }

    public void setLastIPFailure(String lastIPFailure) {
        this.lastIPFailure = lastIPFailure;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }
}
