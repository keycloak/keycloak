package org.keycloak.models;

/**
 * Link between user and authentication provider
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationLinkModel {

    private String authProvider;
    private String authUserId;

    public AuthenticationLinkModel() {};

    public AuthenticationLinkModel(String authProvider, String authUserId) {
        this.authProvider = authProvider;
        this.authUserId = authUserId;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }
}
