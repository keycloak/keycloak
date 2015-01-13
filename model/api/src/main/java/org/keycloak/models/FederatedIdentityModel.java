package org.keycloak.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FederatedIdentityModel {

    private String userId;
    private String identityProvider;
    private String userName;

    public FederatedIdentityModel() {};

    public FederatedIdentityModel(String identityProvider, String userId, String userName) {
        this.userId = userId;
        this.identityProvider = identityProvider;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public String getUserName() {
        return userName;
    }
}
