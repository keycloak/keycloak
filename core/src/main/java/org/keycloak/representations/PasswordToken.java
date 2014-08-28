package org.keycloak.representations;

import org.keycloak.util.Time;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PasswordToken {

    private String realm;
    private String user;
    private int timestamp;

    public PasswordToken() {
    }

    public PasswordToken(String realm, String user) {
        this.realm = realm;
        this.user = user;
        this.timestamp = Time.currentTime();
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

}
