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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Authorization;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;

/**
 * Introspects token accordingly with UMA Bearer Token Profile.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RPTIntrospectionProvider extends AccessTokenIntrospectionProvider<AccessToken> {

    protected static final Logger LOGGER = Logger.getLogger(RPTIntrospectionProvider.class);

    public RPTIntrospectionProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public Response introspect(String tokenStr, EventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
        LOGGER.debug("Introspecting requesting party token");
        try {
            ObjectNode tokenMetadata;
            if (introspectionChecks(tokenStr)) {
                AccessToken accessToken = this.token;
                AccessToken metadata = new AccessToken();

                metadata.id(accessToken.getId());
                metadata.setAcr(accessToken.getAcr());
                metadata.type(accessToken.getType());
                metadata.exp(accessToken.getExp());
                metadata.iat(accessToken.getIat());
                metadata.audience(accessToken.getAudience());
                metadata.nbf(accessToken.getNbf());
                metadata.setRealmAccess(null);
                metadata.setResourceAccess(null);

                tokenMetadata = JsonSerialization.createObjectNode(metadata);
                Authorization authorization = accessToken.getAuthorization();

                if (authorization != null) {
                    Collection permissions;

                    if (authorization.getPermissions() != null) {
                        permissions = authorization.getPermissions().stream().map(UmaPermissionRepresentation::new).collect(Collectors.toSet());
                    } else {
                        permissions = Collections.emptyList();
                    }

                    tokenMetadata.putPOJO("permissions", permissions);
                }
                tokenMetadata.put("active", true);
                eventBuilder.success();
            } else {
                tokenMetadata = JsonSerialization.createObjectNode();
                tokenMetadata.put("active", false);
            }

            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            eventBuilder.detail(Details.REASON, e.getMessage());
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    @Override
    public void close() {

    }

    //todo: we need to avoid creating this class when processing responses. The only reason for that is that
    // UMA defines "resource_id" and "resource_scopes" claims but we use "rsid" and "scopes".
    // To avoid breaking backward compatibility we are just responding with all these claims.
    public static class UmaPermissionRepresentation extends Permission {

        public UmaPermissionRepresentation(Permission permission) {
            setResourceId(permission.getResourceId());
            setResourceName(permission.getResourceName());
            setScopes(permission.getScopes());
        }

        @JsonProperty("resource_id")
        public String getUmaResourceId() {
            return getResourceId();
        }

        @JsonProperty("resource_scopes")
        public Set<String> getUmaResourceScopes() {
            return getScopes();
        }
    }
}
