package org.keycloak.models.sessions.mem.entities;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionKey {

    private final String realm;
    private final String id;

    public UserSessionKey(String realm, String id) {
        this.realm = realm;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserSessionKey key = (UserSessionKey) o;

        if (realm != null ? !realm.equals(key.realm) : key.realm != null) return false;
        if (id != null ? !id.equals(key.id) : key.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = realm != null ? realm.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

}
