/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Empty implementation of the OID4VCLoginProtocol. Its required to be available for integration with the client-registration.
 * Since we do not support any additional functionality(like logging into Keycloak with SIOP-2), its an empty default
 * implementation.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCLoginProtocol implements LoginProtocol {

    public OID4VCLoginProtocol(KeycloakSession session) {
    }

    @Override
    public OID4VCLoginProtocol setSession(KeycloakSession session) {
        return this;
    }

    @Override
    public OID4VCLoginProtocol setRealm(RealmModel realm) {
        return this;
    }

    @Override
    public LoginProtocol setUriInfo(UriInfo uriInfo) {
        return null;
    }

    @Override
    public LoginProtocol setHttpHeaders(HttpHeaders httpHeaders) {
        return null;
    }

    @Override
    public OID4VCLoginProtocol setEventBuilder(EventBuilder event) {
        return this;
    }

    @Override
    public Response authenticated(AuthenticationSessionModel authSession, UserSessionModel userSession,
                                  ClientSessionContext clientSessionCtx) {
        return null;
    }

    @Override
    public Response sendError(AuthenticationSessionModel authSession, Error error) {
        return null;
    }

    @Override
    public Response backchannelLogout(UserSessionModel userSession,
                                      AuthenticatedClientSessionModel clientSession) {
        return null;
    }

    @Override
    public Response frontchannelLogout(UserSessionModel userSession,
                                       AuthenticatedClientSessionModel clientSession) {
        return null;
    }

    @Override
    public Response finishBrowserLogout(UserSessionModel userSession,
                                        AuthenticationSessionModel logoutSession) {
        return null;
    }

    @Override
    public boolean requireReauthentication(UserSessionModel userSession,
                                           AuthenticationSessionModel authSession) {
        return false;
    }

    @Override
    public void close() {
        // nothing to close, just fulfilling the interface.
    }
}