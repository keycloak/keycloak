package org.keycloak.authentication;

import org.keycloak.models.utils.FormMessage;

/**
 * Thrown internally when authenticator wants to fork the current flow.
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ForkFlowException extends AuthenticationFlowException {
    protected FormMessage successMessage;
    protected FormMessage errorMessage;

    public FormMessage getSuccessMessage() {
        return successMessage;
    }

    public FormMessage getErrorMessage() {
        return errorMessage;
    }

    public ForkFlowException(FormMessage successMessage, FormMessage errorMessage) {
        super(AuthenticationFlowError.FORK_FLOW);
        this.successMessage = successMessage;
        this.errorMessage = errorMessage;
    }
}
