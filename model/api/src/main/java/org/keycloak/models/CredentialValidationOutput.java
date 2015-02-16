package org.keycloak.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Output of credential validation
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CredentialValidationOutput {

    private final UserModel authenticatedUser; // authenticated user.
    private final Status authStatus;           // status whether user is authenticated or more steps needed
    private final Map<String, Object> state;   // Additional state related to authentication. It can contain data to be sent back to client or data about used credentials.

    public CredentialValidationOutput(UserModel authenticatedUser, Status authStatus, Map<String, Object> state) {
        this.authenticatedUser = authenticatedUser;
        this.authStatus = authStatus;
        this.state = state;
    }

    public static CredentialValidationOutput failed() {
        return new CredentialValidationOutput(null, CredentialValidationOutput.Status.FAILED, new HashMap<String, Object>());
    }

    public UserModel getAuthenticatedUser() {
        return authenticatedUser;
    }

    public Status getAuthStatus() {
        return authStatus;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public CredentialValidationOutput merge(CredentialValidationOutput that) {
        throw new IllegalStateException("Not supported yet");
    }

    public static enum Status {
        AUTHENTICATED, FAILED, CONTINUE
    }
}
