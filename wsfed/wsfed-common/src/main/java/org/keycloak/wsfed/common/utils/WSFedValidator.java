/*
 * Copyright (C) 2015 Dell, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.wsfed.common.utils;

import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Created on 5/27/15.
 */
public class WSFedValidator {

    private EventBuilder event;
    private RealmModel realm;

    public WSFedValidator(EventBuilder eventBuilder, RealmModel realm) {
        this.event = eventBuilder;
        this.realm = realm;
    }

    private boolean checkSsl(UriInfo uriInfo, ClientConnection clientConnection) {
        if (uriInfo.getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }

    public Response basicChecks(String wsfedAction, UriInfo uriInfo,
                                ClientConnection clientConnection, KeycloakSession session) {
        try {
            if (!checkSsl(uriInfo, clientConnection)) {
                event.event(EventType.LOGIN);
                event.error(Errors.SSL_REQUIRED);
                return ErrorPage.error(session, Messages.HTTPS_REQUIRED);
            }
            if (!realm.isEnabled()) {
                event.event(EventType.LOGIN);
                event.error(Errors.REALM_DISABLED);
                return ErrorPage.error(session, Messages.REALM_NOT_ENABLED);
            }

            if (wsfedAction == null) {
                event.event(EventType.LOGIN);
                event.error(Errors.INVALID_REQUEST);
                return ErrorPage.error(session, Messages.INVALID_REQUEST);
            }
        } catch (Exception e) {
            event.event(EventType.LOGIN_ERROR);
            event.error(e.getLocalizedMessage());
            return ErrorPage.error(session, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST);
        }
        return null;
    }
}
