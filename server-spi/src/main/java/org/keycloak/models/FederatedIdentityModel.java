package org.keycloak.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FederatedIdentityModel {

    private String token;
    private final String userId;
    private final String identityProvider;
    private final String userName;

    public FederatedIdentityModel(String identityProvider, String userId, String userName) {
        this(identityProvider, userId, userName, null);
    }

    public FederatedIdentityModel(String providerId, String userId, String userName, String token) {
        this.identityProvider = providerId;
        this.userId = userId;
        this.userName = userName;
        this.token = token;
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

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
