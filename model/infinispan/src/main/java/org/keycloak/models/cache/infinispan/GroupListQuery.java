package org.keycloak.models.cache.infinispan;

import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.infinispan.entities.AbstractRevisioned;
import org.keycloak.models.cache.infinispan.entities.ClientQuery;
import org.keycloak.models.cache.infinispan.entities.GroupQuery;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupListQuery extends AbstractRevisioned implements GroupQuery {
    private final Set<String> groups;
    private final String realm;
    private final String realmName;

    public GroupListQuery(Long revisioned, String id, RealmModel realm, Set<String> groups) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.realmName = realm.getName();
        this.groups = groups;
    }

    @Override
    public Set<String> getGroups() {
        return groups;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String toString() {
        return "GroupListQuery{" +
                "id='" + getId() + "'" +
                "realmName='" + realmName + '\'' +
                '}';
    }
}
