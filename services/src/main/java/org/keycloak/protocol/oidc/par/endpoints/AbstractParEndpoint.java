/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.par.endpoints;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.cors.Cors;

public abstract class AbstractParEndpoint {

    protected final KeycloakSession session;
    protected final EventBuilder event;
    protected final RealmModel realm;
    protected Cors cors;
    protected ClientModel client;

    public AbstractParEndpoint(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
        realm = session.getContext().getRealm();
    }

    protected void checkSsl() {
        ClientConnection clientConnection = session.getContext().getConnection();

        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    protected void checkRealm() {
        if (!realm.isEnabled()) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.ACCESS_DENIED, "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    protected void authorizeClient() {
        try {
            AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, cors);
            client = clientAuth.getClient();

            this.event.client(client);

            cors.allowedOrigins(session, client);

            if (client == null) {
                throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Client not allowed.", Response.Status.FORBIDDEN);
            }
        } catch (Exception e) {
            throw throwErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Authentication failed.", Response.Status.UNAUTHORIZED);
        }
    }

    protected byte[] getHash(String inputData) {
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("SHA-256").digest(inputData.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Error calculating hash");
        }

        return hash;
    }

    protected CorsErrorResponseException throwErrorResponseException(String error, String detail, Response.Status status) {
        this.event.detail("detail", detail).error(error);
        return new CorsErrorResponseException(cors, error, detail, status);
    }
}
