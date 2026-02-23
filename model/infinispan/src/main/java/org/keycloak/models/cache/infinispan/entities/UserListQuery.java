package org.keycloak.models.cache.infinispan.entities;

import java.util.Set;

import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserListQuery extends AbstractRevisioned implements UserQuery {
    private final String userId;
    private final String realm;

    @Deprecated(forRemoval = true, since = "26.5")
    public UserListQuery(long revisioned, String id, RealmModel realm, Set<String> users) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.userId = users.stream().findAny().orElse(null);
    }

    public UserListQuery(long revisioned, String id, RealmModel realm, String userId) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.userId = userId;
    }

    @Override
    public Set<String> getUsers() {
        return Set.of(userId);
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String toString() {
        return "UserListQuery{" +
                "id='" + getId() + "'" +
                "realm='" + realm + '\'' +
                '}';
    }
}
