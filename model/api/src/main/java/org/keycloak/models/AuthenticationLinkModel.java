package org.keycloak.models;

/**
 * Link between user and authentication provider
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationLinkModel {

    private final String authProvider;
    private final String authUserId;

    public AuthenticationLinkModel(String authProvider, String authUserId) {
        this.authProvider = authProvider;
        this.authUserId = authUserId;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public String getAuthProvider() {
        return authProvider;
    }
}
