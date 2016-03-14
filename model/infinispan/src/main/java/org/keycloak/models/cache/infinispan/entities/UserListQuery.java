package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.RealmModel;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserListQuery extends AbstractRevisioned implements UserQuery {
    private final Set<String> users;
    private final String realm;
    private final String realmName;

    public UserListQuery(Long revisioned, String id, RealmModel realm, Set<String> users) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.realmName = realm.getName();
        this.users = users;
    }

    public UserListQuery(Long revisioned, String id, RealmModel realm, String user) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.realmName = realm.getName();
        this.users = new HashSet<>();
        this.users.add(user);
    }

    @Override
    public Set<String> getUsers() {
        return users;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String toString() {
        return "UserListQuery{" +
                "id='" + getId() + "'" +
                "realmName='" + realmName + '\'' +
                '}';
    }
}
