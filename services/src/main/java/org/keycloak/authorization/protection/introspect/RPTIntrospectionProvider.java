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

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.util.JsonSerialization;

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
            AccessToken accessToken = verifyAccessToken(token);

            ObjectNode tokenMetadata;

            if (accessToken != null) {
                AccessToken metadata = new AccessToken();

                metadata.id(accessToken.getId());
                metadata.setAcr(accessToken.getAcr());
                metadata.type(accessToken.getType());
                metadata.expiration(accessToken.getExpiration());
                metadata.issuedAt(accessToken.getIssuedAt());
                metadata.audience(accessToken.getAudience());
                metadata.notBefore(accessToken.getNotBefore());
                metadata.setRealmAccess(null);
                metadata.setResourceAccess(null);

                tokenMetadata = JsonSerialization.createObjectNode(metadata);
                tokenMetadata.putPOJO("permissions", accessToken.getAuthorization().getPermissions());
            } else {
                tokenMetadata = JsonSerialization.createObjectNode();
            }

            tokenMetadata.put("active", accessToken != null);

            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    @Override
    public void close() {

    }
}
