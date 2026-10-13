/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authentication;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.BruteForceProtector;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AbstractAuthenticationFlowContext {

    /**
     * Current event builder being used
     *
     * @return
     */
    EventBuilder getEvent();

    /**
     * Create a refresh new EventBuilder to use within this context
     *
     * @return
     */
    EventBuilder newEvent();

    /**
     * The current execution in the flow
     *
     * @return
     */
    AuthenticationExecutionModel getExecution();

    /**
     * @return the top level flow (root flow) of this authentication
     */
    AuthenticationFlowModel getTopLevelFlow();

    /**
     * Current realm
     *
     * @return
     */
    RealmModel getRealm();

    /**
     * Information about the IP address from the connecting HTTP client.
     *
     * @return
     */
    ClientConnection getConnection();

    /**
     * UriInfo of the current request
     *
     * @return
     */
    UriInfo getUriInfo();

    /**
     * Current session
     *
     * @return
     */
    KeycloakSession getSession();

    HttpRequest getHttpRequest();
    BruteForceProtector getProtector();


    /**
     * Get any configuration associated with the current execution
     *
     * @return
     */
    AuthenticatorConfigModel getAuthenticatorConfig();

    /**
     * This could be an error message forwarded from another authenticator that is restarting or continuing the flo.  For example
     * the brokering API sends this when the broker failed authentication
     * and we want to continue authentication locally.  forwardedErrorMessage can then be displayed by
     * whatever form is challenging.
     */
    FormMessage getForwardedErrorMessage();

    /**
     * This could be an success message forwarded from another authenticator that is restarting or continuing the flow.  For example
     * a reset password sends an email, then resets the flow with a success message.  forwardedSuccessMessage can then be displayed by
     * whatever form is challenging.
     */
    FormMessage getForwardedSuccessMessage();

    /**
     * This could be an info message forwarded from another authenticator. This info message will be usually displayed only once on the
     * first screen shown to the user during authentication. The authenticator forwarding the info message does not know which the screen would be.
     * For example during user re-authentication, the user should see info message like "Please re-authenticate", but at the beginning of the
     * authentication, it is not 100% clear which screen will be the first shown screen where this message should be displayed
     */
    FormMessage getForwardedInfoMessage();

    /**
     * @see #getForwardedInfoMessage()
     * @param message to be forwarded
     * @param parameters parameters of the message if any
     */
    void setForwardedInfoMessage(String message, Object... parameters);

    /**
     * Generates access code and updates clientsession timestamp
     * Access codes must be included in form action callbacks as a query parameter.
     *
     * @return
     */
    String generateAccessCode();


    AuthenticationExecutionModel.Requirement getCategoryRequirementFromCurrentFlow(String authenticatorCategory);

    /**
     * Mark the current execution as successful.  The flow will then continue
     *
     */
    void success();

    /**
     * Mark the current execution as successful and the auth session sets the
     * credential type in the authentication session as the last credential used
     * to authenticate the user.
     *
     * @param credentialType The credential used to authenticate the user
     */
    void success(String credentialType);

    /**
     * Aborts the current flow
     *
     * @param error
     */
    void failure(AuthenticationFlowError error);

    /**
     * Aborts the current flow.
     *
     * @param error
     * @param response Response that will be sent back to HTTP client
     */
    void failure(AuthenticationFlowError error, Response response);
    
    /**
     * Aborts the current flow.
     *
     * @param error
     * @param response Response that will be sent back to HTTP client
     * @param eventDetails Details about the error event
     * @param userErrorMessage A message describing the error to the user
     */
    void failure(AuthenticationFlowError error, Response response, String eventDetails, String userErrorMessage);

    /**
     * Sends a challenge response back to the HTTP client.  If the current execution requirement is optional, this response will not be
     * sent.  If the current execution requirement is alternative, then this challenge will be sent if no other alternative
     * execution was successful.
     *
     * @param challenge
     */
    void challenge(Response challenge);

    /**
     * Sends the challenge back to the HTTP client regardless of the current execution requirement
     *
     * @param challenge
     */
    void forceChallenge(Response challenge);

    /**
     * Same behavior as forceChallenge(), but the error count in brute force attack detection will be incremented.
     * For example, if a user enters in a bad password, the user is directed to try again, but Keycloak will keep track
     * of how many failures have happened.
     *
     * @param error
     * @param challenge
     */
    void failureChallenge(AuthenticationFlowError error, Response challenge);

    /**
     * There was no failure or challenge.  The authenticator was attempted, but not fulfilled.  If the current execution
     * requirement is alternative or optional, then this status is ignored by the flow.
     *
     */
    void attempted();

    /**
     * Get the current status of the current execution.
     *
     * @return may return null if not set yet.
     */
    FlowStatus getStatus();

    /**
     * Get the error condition of a failed execution.
     *
     * @return may return null if there was no error
     */
    AuthenticationFlowError getError();
    
    
    /**
     * Get details of the event that caused an error
     * @return may return null if not set
     */
    String getEventDetails();
    
    /**
     * A custom error message that can be displayed to the user
     * @return Optional error message
     */
    String getUserErrorMessage();
}
