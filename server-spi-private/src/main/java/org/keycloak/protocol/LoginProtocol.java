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

package org.keycloak.protocol;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface LoginProtocol extends Provider {

    public static enum Error {

        /**
         * Login cancelled by the user
         */
        CANCELLED_BY_USER,
        /**
         * Consent denied by the user
         */
        CONSENT_DENIED,
        /**
         * Passive authentication mode requested but nobody is logged in
         */
        PASSIVE_LOGIN_REQUIRED,
        /**
         * Passive authentication mode requested, user is logged in, but some other user interaction is necessary (eg. some required login actions exist or Consent approval is necessary for logged in
         * user)
         */
        PASSIVE_INTERACTION_REQUIRED;
    }

    LoginProtocol setSession(KeycloakSession session);

    LoginProtocol setRealm(RealmModel realm);

    LoginProtocol setUriInfo(UriInfo uriInfo);

    LoginProtocol setHttpHeaders(HttpHeaders headers);

    LoginProtocol setEventBuilder(EventBuilder event);

    Response authenticated(UserSessionModel userSession, ClientSessionCode accessCode);

    Response sendError(ClientSessionModel clientSession, Error error);

    void backchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession);
    Response frontchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession);
    Response finishLogout(UserSessionModel userSession);

    /**
     * @param userSession
     * @param clientSession
     * @return true if SSO cookie authentication can't be used. User will need to "actively" reauthenticate
     */
    boolean requireReauthentication(UserSessionModel userSession, ClientSessionModel clientSession);

}
