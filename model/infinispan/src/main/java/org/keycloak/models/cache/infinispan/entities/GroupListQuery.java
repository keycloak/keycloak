package org.keycloak.models.cache.infinispan.entities;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupListQuery extends AbstractRevisioned implements GroupQuery {
    private final String realm;
    private final Map<String, Set<String>> searchKeys;

    public GroupListQuery(Long revisioned, String id, RealmModel realm, String searchKey, Set<String> result) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.searchKeys = new HashMap<>();
        this.searchKeys.put(searchKey, result);
    }

    public GroupListQuery(Long revisioned, String id, RealmModel realm, String searchKey, Set<String> result, GroupListQuery previous) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.searchKeys = new HashMap<>();
        this.searchKeys.putAll(previous.searchKeys);
        this.searchKeys.put(searchKey, result);
    }

    public GroupListQuery(Long revisioned, String id, RealmModel realm, Set<String> ids) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.searchKeys = new HashMap<>();
        this.searchKeys.put(id, ids);
    }

    @Override
    public Set<String> getGroups() {
        Collection<Set<String>> values = searchKeys.values();

        if (values.isEmpty()) {
            return Set.of();
        }

        return values.stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    public Set<String> getGroups(String searchKey) {
        return searchKeys.get(searchKey);
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String toString() {
        return "GroupListQuery{" +
                "id='" + getId() + "'" +
                "realm='" + realm + '\'' +
                '}';
    }
}
