package org.keycloak.authentication;

/**
 * Status of an execution/authenticator in a Authentication Flow
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public enum FlowStatus {
    /**
     * Successful execution
     */
    SUCCESS,

    /**
     * Execution offered a challenge.  Optional executions will ignore this challenge.  Alternative executions may
     * ignore the challenge depending on the status of other executions in the flow.
     *
     */
    CHALLENGE,

    /**
     * Irregardless of the execution's requirement, this challenge will be sent to the user.
     *
     */
    FORCE_CHALLENGE,

    /**
     * Flow will be aborted and a Response provided by the execution will be sent.
     *
     */
    FAILURE_CHALLENGE,

    /**
     * Flow will be aborted.
     *
     */
    FAILED,

    /**
     * This is not an error condition.  Execution was attempted, but the authenticator is unable to process the request.  An example of this is if
     * a Kerberos authenticator did not see a negotiate header.  There was no error, but the execution was attempted.
     *
     */
    ATTEMPTED,

    /**
     * This flow is being forked.  The current client session is being cloned, reset, and redirected to browser login.
     *
     */
    FORK

}
