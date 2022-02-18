package org.keycloak.storage;

import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleProvider;
import org.keycloak.provider.Provider;

public interface DatastoreProvider extends Provider {

    public ClientScopeProvider clientScopes();

    public ClientProvider clients();

    public GroupProvider groups();

    public RealmProvider realms();

    public RoleProvider roles();
    
}
