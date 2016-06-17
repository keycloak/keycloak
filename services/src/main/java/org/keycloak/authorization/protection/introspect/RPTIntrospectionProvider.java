/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.protection.introspect;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Introspects token accordingly with UMA Bearer Token Profile.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RPTIntrospectionProvider extends AccessTokenIntrospectionProvider {

    protected static final Logger LOGGER = Logger.getLogger(RPTIntrospectionProvider.class);

    public RPTIntrospectionProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public Response introspect(String token) {
        LOGGER.debug("Introspecting requesting party token");
        try {
            AccessToken requestingPartyToken = toAccessToken(token);
            boolean active = isActive(requestingPartyToken);
            ObjectNode tokenMetadata;

            if (active) {
                LOGGER.debug("Token is active");
                AccessToken introspect = new AccessToken();
                introspect.type(requestingPartyToken.getType());
                introspect.expiration(requestingPartyToken.getExpiration());
                introspect.issuedAt(requestingPartyToken.getIssuedAt());
                introspect.audience(requestingPartyToken.getAudience());
                introspect.notBefore(requestingPartyToken.getNotBefore());
                introspect.setRealmAccess(null);
                introspect.setResourceAccess(null);
                tokenMetadata = JsonSerialization.createObjectNode(introspect);
                tokenMetadata.putPOJO("permissions", requestingPartyToken.getAuthorization().getPermissions());
            } else {
                LOGGER.debug("Token is not active");
                tokenMetadata = JsonSerialization.createObjectNode();
            }

            tokenMetadata.put("active", active);

            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    private boolean isActive(AccessToken requestingPartyToken) {
        Authorization authorization = requestingPartyToken.getAuthorization();
        return requestingPartyToken.isActive() && authorization != null && authorization.getPermissions() != null && !authorization.getPermissions().isEmpty();
    }

    @Override
    public void close() {

    }
}
