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

import java.net.URI;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionConfigModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Interface that encapsulates information about the current required action
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RequiredActionContext {
    enum Status {
        CHALLENGE,
        SUCCESS,
        CANCELLED,
        IGNORE,
        FAILURE
    }

    enum KcActionStatus {
        SUCCESS,
        CANCELLED,
        ERROR
    }

    String getAction();

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
     * The configuration of the current required action. Returns {@literal null} if the current required action is not configurable.
     * @return
     */
    RequiredActionConfigModel getConfig();

    /**
     * Generates access code and updates clientsession timestamp
     * Access codes must be included in form action callbacks as a query parameter.
     *
     * @return
     */
    String generateCode();

    Status getStatus();

    String getErrorMessage();

    /**
     * Send a challenge Response back to user
     *
     * @param response
     */
    void challenge(Response response);

    /**
     * Abort the authentication with an error, optionally with an erroMessage.
     *
     */
    void failure(String errorMessage);

    /**
     * Abort the authentication with an error
     *
     */
    default void failure() {
        failure(null);
    }

    /**
     * Mark this required action as successful.  The required action will be removed from the UserModel
     *
     */
    void success();

    /**
     * Mark this action as cancelled. Can be only used in AIA
     */
    void cancel();

    /**
     * Ignore this required action and go onto the next, or complete the flow.
     *
     */
    void ignore();

}
