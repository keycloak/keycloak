package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.RealmModel;

import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleListQuery extends AbstractRevisioned implements RoleQuery, InClient {
    private final Set<String> roles;
    private final String realm;
    private final String realmName;
    private String client;

    public RoleListQuery(Long revisioned, String id, RealmModel realm, Set<String> roles) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.realmName = realm.getName();
        this.roles = roles;
    }

    public RoleListQuery(Long revisioned, String id, RealmModel realm, String role) {
        super(revisioned, id);
        this.realm = realm.getId();
        this.realmName = realm.getName();
        this.roles = new HashSet<>();
        this.roles.add(role);
    }

    public RoleListQuery(Long revision, String id, RealmModel realm, Set<String> roles, String client) {
        this(revision, id, realm, roles);
        this.client = client;
    }

    public RoleListQuery(Long revision, String id, RealmModel realm, String role, String client) {
        this(revision, id, realm, role);
        this.client = client;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getClientId() {
        return client;
    }

    @Override
    public String toString() {
        return "RoleListQuery{" +
                "id='" + getId() + "'" +
                ", realmName='" + realmName + '\'' +
                ", clientUuid='" + client + '\'' +
                '}';
    }
}
