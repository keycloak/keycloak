package org.keycloak.models.sessions.infinispan.entities;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginFailureKey {

    private final String realm;
    private final String username;

    public LoginFailureKey(String realm, String username) {
        this.realm = realm;
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoginFailureKey key = (LoginFailureKey) o;

        if (realm != null ? !realm.equals(key.realm) : key.realm != null) return false;
        if (username != null ? !username.equals(key.username) : key.username != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = realm != null ? realm.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        return result;
    }

}
