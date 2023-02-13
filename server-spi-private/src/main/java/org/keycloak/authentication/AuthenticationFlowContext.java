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

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.URI;
import java.util.List;

/**
 * This interface encapsulates information about an execution in an AuthenticationFlow.  It is also used to set
 * the status of the execution being performed.
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AuthenticationFlowContext extends AbstractAuthenticationFlowContext {

    /**
     * Current user attached to this flow.  It can return null if no user has been identified yet
     *
     * @return
     */
    UserModel getUser();

    /**
     * Attach a specific user to this flow.
     *
     * @param user
     */
    void setUser(UserModel user);

    List<AuthenticationSelectionOption> getAuthenticationSelections();

    void setAuthenticationSelections(List<AuthenticationSelectionOption>  credentialAuthExecMap);

    /**
     * Clear the user from the flow.
     */
    void clearUser();

    void attachUserSession(UserSessionModel userSession);


    /**
     * AuthenticationSessionModel attached to this flow
     *
     * @return
     */
    AuthenticationSessionModel getAuthenticationSession();

    /**
     * @return current flow path (EG. authenticate, reset-credentials)
     */
    String getFlowPath();

    /**
     * Create a Freemarker form builder that presets the user, action URI, and a generated access code
     *
     * @return
     */
    LoginFormsProvider form();

    /**
     * Get the action URL for the required action.
     *
     * @param code authentication session access code
     * @return
     */
    URI getActionUrl(String code);

    /**
     * Get the action URL for the action token executor.
     *
     * @param tokenString String representation (JWT) of action token
     * @return
     */
    URI getActionTokenUrl(String tokenString);

    /**
     * Get the refresh URL for the required action.
     *
     * @return
     */
    URI getRefreshExecutionUrl();

    /**
     * Get the refresh URL for the flow.
     *
     * @param authSessionIdParam will include auth_session query param for clients that don't process cookies
     * @return
     */
    URI getRefreshUrl(boolean authSessionIdParam);

    /**
     * End the flow and redirect browser based on protocol specific respones.  This should only be executed
     * in browser-based flows.
     *
     */
    void cancelLogin();

    /**
     * Reset the current flow to the beginning and restarts it.
     *
     */
    void resetFlow();

    /**
     * Reset the current flow to the beginning and restarts it. Allows to add additional listener, which is triggered after flow restarted
     *
     */
    void resetFlow(Runnable afterResetListener);

    /**
     * Fork the current flow.  The authentication session will be cloned and set to point at the realm's browser login flow.  The Response will be the result
     * of this fork.  The previous flow will still be set at the current execution.  This is used by reset password when it sends an email.
     * It sends an email linking to the current flow and redirects the browser to a new browser login flow.
     *
     *
     *
     * @return
     */
    void fork();

    /**
     * Fork the current flow.  The authentication session will be cloned and set to point at the realm's browser login flow.  The Response will be the result
     * of this fork.  The previous flow will still be set at the current execution.  This is used by reset password when it sends an email.
     * It sends an email linking to the current flow and redirects the browser to a new browser login flow.
     *
     * This method will set up a success message that will be displayed in the first page of the new flow
     *
     * @param message Corresponds to raw text or a message property defined in a message bundle
     */
    void forkWithSuccessMessage(FormMessage message);
    /**
     * Fork the current flow.  The authentication session will be cloned and set to point at the realm's browser login flow.  The Response will be the result
     * of this fork.  The previous flow will still be set at the current execution.  This is used by reset password when it sends an email.
     * It sends an email linking to the current flow and redirects the browser to a new browser login flow.
     *
     * This method will set up an error message that will be displayed in the first page of the new flow
     *
     * @param message Corresponds to raw text or a message property defined in a message bundle
     */
    void forkWithErrorMessage(FormMessage message);
}
