package org.keycloak.spi.authentication;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthResult {

    // Status of authentication
    private final AuthProviderStatus authProviderStatus;

    // Provider, which authenticated user
    private String providerName;

    // filled usually only in case of successful authentication and just with some Authentication providers
    private AuthenticatedUser authenticatedUser;

    public AuthResult(AuthProviderStatus authProviderStatus) {
        this.authProviderStatus = authProviderStatus;
    }

    public AuthResult setProviderName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    public AuthResult setUser(AuthenticatedUser user) {
        this.authenticatedUser = user;
        return this;
    }

    public AuthProviderStatus getAuthProviderStatus() {
        return authProviderStatus;
    }

    public String getProviderName() {
        return providerName;
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser;
    }


}
