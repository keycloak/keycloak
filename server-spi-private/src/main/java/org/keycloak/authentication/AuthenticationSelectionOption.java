package org.keycloak.authentication;

import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;

public class AuthenticationSelectionOption {

    private final KeycloakSession session;
    private final AuthenticationExecutionModel authExec;

    public AuthenticationSelectionOption(KeycloakSession session, AuthenticationExecutionModel authExec) {
        this.session = session;
        this.authExec = authExec;
    }


    public AuthenticationExecutionModel getAuthenticationExecution() {
        return authExec;
    }

    public String getAuthExecId(){
        return authExec.getId();
    }

    public String getAuthExecName() {
        return authExec.getAuthenticator();
    }

    public String getAuthExecDisplayName() {
        // TODO: Retrieve the displayName for the authenticator from the AuthenticationFactory
        // TODO: Retrieve icon CSS style
        // TODO: Should be addressed as part of https://issues.redhat.com/browse/KEYCLOAK-12185
        return getAuthExecName();
    }


    @Override
    public String toString() {
        return " authSelection - " + authExec.getAuthenticator();
    }
}
