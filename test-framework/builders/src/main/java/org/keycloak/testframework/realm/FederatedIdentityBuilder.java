package org.keycloak.testframework.realm;

import org.keycloak.representations.idm.FederatedIdentityRepresentation;

public class FederatedIdentityBuilder extends Builder<FederatedIdentityRepresentation> {

    private FederatedIdentityBuilder(FederatedIdentityRepresentation rep) {
        super(rep);
    }

    public static FederatedIdentityBuilder create() {
        return new FederatedIdentityBuilder(new FederatedIdentityRepresentation());
    }

    public static FederatedIdentityBuilder create(String identityProvider, String userId, String userName) {
        return new FederatedIdentityBuilder(new FederatedIdentityRepresentation()).identityProvider(identityProvider).userId(userId).userName(userName);
    }

    public static FederatedIdentityBuilder update(FederatedIdentityRepresentation rep) {
        return new FederatedIdentityBuilder(rep);
    }

    public FederatedIdentityBuilder identityProvider(String identityProvider) {
        rep.setIdentityProvider(identityProvider);
        return this;
    }

    public FederatedIdentityBuilder userId(String userId) {
        rep.setUserId(userId);
        return this;
    }

    public FederatedIdentityBuilder userName(String userName) {
        rep.setUserName(userName);
        return this;
    }

}
