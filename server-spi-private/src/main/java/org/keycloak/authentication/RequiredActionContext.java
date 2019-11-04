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

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * Interface that encapsulates current information about the current requred action
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RequiredActionContext {
    enum Status {
        CHALLENGE,
        SUCCESS,
        IGNORE,
        FAILURE
    }

    enum KcActionStatus {
        SUCCESS,
        CANCELLED,
        ERROR
    }

    /**
     * Get the action URL for the required action.
     *
     * @param code client sessino access code
     * @return
     */
    URI getActionUrl(String code);

    /**
     * Get the action URL for the required action.  This auto-generates the access code.
     *
     * @return
     */
    URI getActionUrl();

    /**
     * Get the action URL for the required action.  This auto-generates the access code.
     *
     * @param authSessionIdParam if true, will embed session id as query param.  Useful for clients that don't support cookies (i.e. console)
     *
     * @return
     */
    URI getActionUrl(boolean authSessionIdParam);

    /**
     * Create a Freemarker form builder that presets the user, action URI, and a generated access code
     *
     * @return
     */
    LoginFormsProvider form();


    /**
     * If challenge has been sent this returns the JAX-RS Response
     *
     * @return
     */
    Response getChallenge();


    /**
     * Current event builder being used
     *
     * @return
     */
    EventBuilder getEvent();

    /**
     * Current user
     *
     * @return
     */
    UserModel getUser();
    RealmModel getRealm();
    AuthenticationSessionModel getAuthenticationSession();
    ClientConnection getConnection();
    UriInfo getUriInfo();
    KeycloakSession getSession();
    HttpRequest getHttpRequest();

    /**
     * Generates access code and updates clientsession timestamp
     * Access codes must be included in form action callbacks as a query parameter.
     *
     * @return
     */
    String generateCode();

    Status getStatus();

    /**
     * Send a challenge Response back to user
     *
     * @param response
     */
    void challenge(Response response);

    /**
     * Abort the authentication with an error
     *
     */
    void failure();

    /**
     * Mark this required action as successful.  The required action will be removed from the UserModel
     *
     */
    void success();

    /**
     * Ignore this required action and go onto the next, or complete the flow.
     *
     */
    void ignore();

}
