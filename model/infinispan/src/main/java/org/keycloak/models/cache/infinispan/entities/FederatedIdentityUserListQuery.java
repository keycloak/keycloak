package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.RealmModel;

public class FederatedIdentityUserListQuery extends UserListQuery implements InIdentityProvider {

    private final String identityProvider;

    public FederatedIdentityUserListQuery(long revisioned, String id, RealmModel realm, String userId, String identityProvider) {
        super(revisioned, id, realm, userId);
        this.identityProvider = identityProvider;
    }

    @Override
    public boolean contains(String providerId) {
        return identityProvider != null && identityProvider.equals(providerId);
    }
}
