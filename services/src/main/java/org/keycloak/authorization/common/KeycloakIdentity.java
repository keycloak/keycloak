/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.authorization.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.util.Tokens;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class KeycloakIdentity implements Identity {

    protected final AccessToken accessToken;
    protected final RealmModel realm;
    protected final KeycloakSession keycloakSession;
    protected final Attributes attributes;
    private final boolean resourceServer;
    private final String id;

    public KeycloakIdentity(KeycloakSession keycloakSession) {
        this(Tokens.getAccessToken(keycloakSession), keycloakSession);
    }

    public KeycloakIdentity(KeycloakSession keycloakSession, IDToken token) {
        this(token, keycloakSession, keycloakSession.getContext().getRealm());
    }

    public KeycloakIdentity(IDToken token, KeycloakSession keycloakSession, RealmModel realm) {
        if (token == null) {
            throw new ErrorResponseException("invalid_bearer_token", "Could not obtain bearer access_token from request.", Status.FORBIDDEN);
        }
        if (keycloakSession == null) {
            throw new ErrorResponseException("no_keycloak_session", "No keycloak session", Status.FORBIDDEN);
        }
        if (realm == null) {
            throw new ErrorResponseException("no_keycloak_session", "No realm set", Status.FORBIDDEN);
        }
        this.keycloakSession = keycloakSession;
        this.realm = realm;

        Map<String, Collection<String>> attributes = new HashMap<>();

        try {
            ObjectNode objectNode = JsonSerialization.createObjectNode(token);
            Iterator<String> iterator = objectNode.fieldNames();

            while (iterator.hasNext()) {
                String fieldName = iterator.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                List<String> values = new ArrayList<>();

                if (fieldValue.isArray()) {
                    Iterator<JsonNode> valueIterator = fieldValue.iterator();

                    while (valueIterator.hasNext()) {
                        values.add(valueIterator.next().asText());
                    }
                } else {
                    // If the claim is key value pair then just take it as is to attributes.
                    if(!fieldValue.isObject()) {
                        String value = fieldValue.asText();

                        if (StringUtil.isNullOrEmpty(value)) {
                            continue;
                        }
                        values.add(value);
                    }
                    // otherwise, the claim is a JSON object, turn it into json String, so it'll be able to evaluate it later
                    // in the regex policy evaluator
                    else
                    {
                        values.add(fieldValue.toString());
                    }
                }

                if (!values.isEmpty()) {
                    attributes.put(fieldName, values);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while reading attributes from security token.", e);
        }

        if (token instanceof AccessToken) {
            this.accessToken = AccessToken.class.cast(token);
        } else {
            UserSessionProvider sessions = keycloakSession.sessions();
            UserSessionModel userSession = sessions.getUserSession(realm, token.getSessionState());

            if (userSession == null) {
                userSession = sessions.getOfflineUserSession(realm, token.getSessionState());
            }

            if (userSession == null) {
                throw new RuntimeException("No active session associated with the token");
            }

            if (AuthenticationManager.isSessionValid(realm, userSession) && token.isIssuedBeforeSessionStart(userSession.getStarted())) {
                throw new RuntimeException("Invalid token");
            }

            ClientModel client = realm.getClientByClientId(token.getIssuedFor());
            AuthenticatedClientSessionModel clientSessionModel = userSession.getAuthenticatedClientSessionByClient(client.getId());

            ClientSessionContext clientSessionCtx = DefaultClientSessionContext.fromClientSessionScopeParameter(clientSessionModel, keycloakSession);
            this.accessToken = new TokenManager().createClientAccessToken(keycloakSession, realm, client, userSession.getUser(), userSession, clientSessionCtx);
        }

        AccessToken.Access realmAccess = this.accessToken.getRealmAccess();

        if (realmAccess != null) {
            attributes.put("kc.realm.roles", realmAccess.getRoles());
        }

        Map<String, AccessToken.Access> resourceAccess = this.accessToken.getResourceAccess();

        if (resourceAccess != null) {
            resourceAccess.forEach((clientId, access) -> attributes.put("kc.client." + clientId + ".roles", access.getRoles()));
        }

        ClientModel clientModel = getTargetClient();
        UserModel clientUser = null;

        if (clientModel != null && clientModel.isServiceAccountsEnabled()) {
            clientUser = this.keycloakSession.users().getServiceAccount(clientModel);
        }

        UserModel userSession = getUserFromToken();

        this.resourceServer = clientUser != null && userSession.getId().equals(clientUser.getId());

        if (resourceServer) {
            this.id = clientModel.getId();
        } else {
            this.id = userSession.getId();
        }

        this.attributes = Attributes.from(attributes);
    }

    public KeycloakIdentity(AccessToken accessToken, KeycloakSession keycloakSession) {
        this(accessToken, keycloakSession, keycloakSession.getContext().getRealm());
    }

    public KeycloakIdentity(AccessToken accessToken, KeycloakSession keycloakSession, RealmModel realm) {
        if (accessToken == null) {
            throw new ErrorResponseException("invalid_bearer_token", "Could not obtain bearer access_token from request.", Status.FORBIDDEN);
        }
        if (keycloakSession == null) {
            throw new ErrorResponseException("no_keycloak_session", "No keycloak session", Status.FORBIDDEN);
        }
        if (realm == null) {
            throw new ErrorResponseException("no_keycloak_session", "No realm set", Status.FORBIDDEN);
        }
        this.accessToken = accessToken;
        this.keycloakSession = keycloakSession;
        this.realm = realm;

        Map<String, Collection<String>> attributes = new HashMap<>();

        try {
            ObjectNode objectNode = JsonSerialization.createObjectNode(this.accessToken);
            Iterator<String> iterator = objectNode.fieldNames();

            while (iterator.hasNext()) {
                String fieldName = iterator.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                List<String> values = new ArrayList<>();

                if (fieldValue.isArray()) {
                    Iterator<JsonNode> valueIterator = fieldValue.iterator();

                    while (valueIterator.hasNext()) {
                        values.add(valueIterator.next().asText());
                    }
                } else if (fieldValue.isObject()) {
                    values.add(fieldValue.toString());
                } else {
                    String value = fieldValue.asText();

                    if (StringUtil.isNullOrEmpty(value)) {
                        continue;
                    }

                    values.add(value);
                }

                if (!values.isEmpty()) {
                    attributes.put(fieldName, values);
                }
            }

            AccessToken.Access realmAccess = accessToken.getRealmAccess();

            if (realmAccess != null) {
                attributes.put("kc.realm.roles", realmAccess.getRoles());
            }

            Map<String, AccessToken.Access> resourceAccess = accessToken.getResourceAccess();

            if (resourceAccess != null) {
                resourceAccess.forEach((clientId, access) -> attributes.put("kc.client." + clientId + ".roles", access.getRoles()));
            }

            ClientModel clientModel = getTargetClient();
            UserModel clientUser = null;

            if (clientModel != null && clientModel.isServiceAccountsEnabled()) {
                clientUser = this.keycloakSession.users().getServiceAccount(clientModel);
            }

            UserModel userSession = getUserFromToken();
            if (userSession == null) {
                throw new IllegalArgumentException("User from token not found");
            }

            this.resourceServer = clientUser != null && userSession.getId().equals(clientUser.getId());

            if (resourceServer) {
                this.id = clientModel.getId();
            } else {
                this.id = userSession.getId();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while reading attributes from security token.", e);
        }

        this.attributes = Attributes.from(attributes);
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Attributes getAttributes() {
        return this.attributes;
    }

    public AccessToken getAccessToken() {
        return this.accessToken;
    }

    public boolean isResourceServer() {
        return this.resourceServer;
    }

    private ClientModel getTargetClient() {
        if (this.accessToken.getIssuedFor() != null) {
            return realm.getClientByClientId(accessToken.getIssuedFor());
        }

        if (this.accessToken.getAudience() != null && this.accessToken.getAudience().length > 0) {
            String audience = this.accessToken.getAudience()[0];
            return realm.getClientByClientId(audience);
        }

        return null;
    }

    private UserModel getUserFromToken() {
        if (accessToken.getSessionState() == null) {
            return TokenManager.lookupUserFromStatelessToken(keycloakSession, realm, accessToken);
        }

        // Avoid further loookup if verified userSession already set in the context
        UserSessionModel userSession = keycloakSession.getContext().getUserSession();
        if (userSession != null && accessToken.getSessionState().equals(userSession.getId())) {
            return userSession.getUser();
        }

        UserSessionProvider sessions = keycloakSession.sessions();
        userSession = sessions.getUserSession(realm, accessToken.getSessionState());

        if (userSession == null) {
            userSession = sessions.getOfflineUserSession(realm, accessToken.getSessionState());
        }
        if (AuthenticationManager.isSessionValid(realm, userSession) && accessToken.isIssuedBeforeSessionStart(userSession.getStarted())) {
            return null;
        }
        return userSession.getUser();
    }
}
