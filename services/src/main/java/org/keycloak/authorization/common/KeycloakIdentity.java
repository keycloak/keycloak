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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.keycloak.services.util.DefaultClientSessionContext;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        requireToken(token);
        this.keycloakSession = requireSession(keycloakSession);
        this.realm = requireRealm(realm);

        UserModel user = null;
        AccessToken accessToken;
        if (token instanceof AccessToken) {
            accessToken = (AccessToken) token;
        } else {
            UserSessionProvider sessions = keycloakSession.sessions();
            UserSessionModel userSession = sessions.getUserSession(realm, token.getSessionState());

            if (userSession == null) {
                userSession = sessions.getOfflineUserSession(realm, token.getSessionState());
            }

            if (userSession == null) {
                throw new RuntimeException("No active session associated with the token");
            }

            user = userSession.getUser();

            ClientModel client = realm.getClientByClientId(token.getIssuedFor());
            AuthenticatedClientSessionModel clientSessionModel =
                    userSession.getAuthenticatedClientSessions().get(client.getId());

            ClientSessionContext clientSessionCtx =
                    DefaultClientSessionContext.fromClientSessionScopeParameter(clientSessionModel, keycloakSession);
            accessToken = new TokenManager().createClientAccessToken(keycloakSession, realm, client, user, userSession,
                    clientSessionCtx);
        }

        accessToken = new TokenManager().completeIntrospectableAccessToken(accessToken, keycloakSession, realm);
        Map<String, Collection<String>> attributes = createAttributesFromToken(accessToken);

        this.accessToken = accessToken;

        if (user == null) {
            user = getUserFromToken();
        }

        AccessToken.Access realmAccess = this.accessToken.getRealmAccess();

        if (realmAccess != null) {
            attributes.put("kc.realm.roles", realmAccess.getRoles());
        }

        Map<String, AccessToken.Access> resourceAccess = this.accessToken.getResourceAccess();

        if (resourceAccess != null) {
            resourceAccess.forEach(
                    (clientId, access) -> attributes.put("kc.client." + clientId + ".roles", access.getRoles()));
        }

        ClientModel clientModel = getTargetClient();
        UserModel clientUser = null;

        if (clientModel != null) {
            clientUser = this.keycloakSession.users().getServiceAccount(clientModel);
        }

        this.resourceServer = clientUser != null && user.getId().equals(clientUser.getId());

        if (resourceServer) {
            this.id = clientModel.getId();
        } else {
            this.id = user.getId();
        }

        this.attributes = Attributes.from(attributes);
    }

    public KeycloakIdentity(AccessToken accessToken, KeycloakSession keycloakSession) {
        this(requireToken(accessToken), requireSession(keycloakSession),
                requireRealm(keycloakSession.getContext().getRealm()));
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

    private static RealmModel requireRealm(RealmModel realm) {
        if (realm == null) {
            throw new ErrorResponseException("no_keycloak_session", "No realm set", Status.FORBIDDEN);
        }
        return realm;
    }

    private static KeycloakSession requireSession(KeycloakSession keycloakSession) {
        if (keycloakSession == null) {
            throw new ErrorResponseException("no_keycloak_session", "No keycloak session", Status.FORBIDDEN);
        }
        return keycloakSession;
    }

    private static <T extends IDToken> T requireToken(T token) {
        if (token == null) {
            throw new ErrorResponseException("invalid_bearer_token",
                    "Could not obtain bearer access_token from request.", Status.FORBIDDEN);
        }
        return token;
    }

    private static Map<String, Collection<String>> createAttributesFromToken(IDToken token) {
        Map<String, Collection<String>> attributes = new HashMap<>();

        try {
            ObjectNode objectNode = JsonSerialization.createObjectNode(token);
            Iterator<String> iterator = objectNode.fieldNames();

            while (iterator.hasNext()) {
                String fieldName = iterator.next();
                JsonNode fieldValue = objectNode.get(fieldName);
                List<String> values = new ArrayList<>();

                if (fieldValue.isArray()) {
                    for (JsonNode jsonNode : fieldValue) {
                        values.add(jsonNode.asText());
                    }
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
        } catch (Exception e) {
            throw new RuntimeException("Error while reading attributes from security token.", e);
        }
        return attributes;
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

        UserSessionProvider sessions = keycloakSession.sessions();
        UserSessionModel userSession = sessions.getUserSession(realm, accessToken.getSessionState());

        if (userSession == null) {
            userSession = sessions.getOfflineUserSession(realm, accessToken.getSessionState());
        }

        return userSession.getUser();
    }
}
