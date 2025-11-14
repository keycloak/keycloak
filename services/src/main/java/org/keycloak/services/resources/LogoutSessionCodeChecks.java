/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.resources;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.RootAuthenticationSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LogoutSessionCodeChecks extends SessionCodeChecks {

    public LogoutSessionCodeChecks(RealmModel realm, UriInfo uriInfo, HttpRequest request, ClientConnection clientConnection, KeycloakSession session, EventBuilder event,
                                   String code, String clientId, String tabId) {
        super(realm, uriInfo, request, clientConnection, session, event, null, code, null, clientId, tabId, null, null);
    }


    @Override
    protected void setClientToEvent(ClientModel client) {
        // Skip sending client to logout event
    }

    @Override
    protected Response restartAuthenticationSessionFromCookie(RootAuthenticationSessionModel existingRootSession) {
        // Skip restarting authentication session from KC_RESTART cookie during logout
        getEvent().error(Errors.SESSION_EXPIRED);
        return ErrorPage.error(getSession(), null, Response.Status.BAD_REQUEST, Messages.FAILED_LOGOUT);
    }

    @Override
    protected boolean isActionActive(ClientSessionCode.ActionType actionType) {
        if (!getClientCode().isActionActive(actionType)) {
            getEvent().clone().error(Errors.EXPIRED_CODE);
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkClientDisabled(ClientModel client) {
        return !client.isEnabled() && getClientCode() != null;
    }
}
