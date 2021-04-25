/*
 * Copyright 2021  Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants.ciba.endpoints;

import javax.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.services.ErrorResponseException;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractCibaEndpoint {

    protected final KeycloakSession session;
    protected final EventBuilder event;
    protected final RealmModel realm;

    public AbstractCibaEndpoint(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
        realm = session.getContext().getRealm();
    }

    protected ClientModel authenticateClient() {
        checkSsl();
        checkRealm();

        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, null);
        ClientModel client = clientAuth.getClient();

        if (client.isBearerOnly()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }

        if (!realm.getCibaPolicy().isOIDCCIBAGrantEnabled(client)) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT,
                    "Client not allowed OIDC CIBA Grant", Response.Status.BAD_REQUEST);
        }

        event.client(client);

        return client;
    }

    protected void checkSsl() {
        ClientConnection clientConnection = session.getContext().getContextObject(ClientConnection.class);
        RealmModel realm = session.getContext().getRealm();

        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    protected void checkRealm() {
        RealmModel realm = session.getContext().getRealm();

        if (!realm.isEnabled()) {
            throw new ErrorResponseException("access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }
}
