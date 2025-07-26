/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oauth2.OAuth2WellKnownProviderFactory;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.wellknown.WellKnownProvider;

@Provider
@Path("/")
public class ServerMetadataResource {

    protected static final Logger logger = Logger.getLogger(ServerMetadataResource.class);

    @Context
    protected KeycloakSession session;

    @OPTIONS
    @Path("/.well-known/oauth-authorization-server/realms/{realm}")
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response getOAuth2AuthorizationServerWellKnownVersionPreflight(final @PathParam("realm") String name) {
        return Cors.builder().allowedMethods("GET").preflight().auth().add(Response.ok());
    }

    @GET
    @Path("/.well-known/oauth-authorization-server/realms/{realm}")
    @Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response getOAuth2AuthorizationServerWellKnown(final @PathParam("realm") String name) {
        resolveRealmAndUpdateSession(name);
        checkSsl(session.getContext().getRealm());

        WellKnownProvider wellKnown = session.getProvider(WellKnownProvider.class, OAuth2WellKnownProviderFactory.PROVIDER_ID);
        logger.tracef("Use provider with ID '%s'", OAuth2WellKnownProviderFactory.PROVIDER_ID);

        if (wellKnown != null) {
            Response.ResponseBuilder responseBuilder = Response.ok(wellKnown.getConfig()).cacheControl(CacheControlUtil.noCache());
            return Cors.builder().allowAllOrigins().auth().add(responseBuilder);
        }

        throw new NotFoundException();
    }

    public static UriBuilder wellKnownProviderUrl(UriBuilder builder) {
        return builder.path(ServerMetadataResource.class, "getOAuth2AuthorizationServerWellKnown");
    }

    private void resolveRealmAndUpdateSession(String realmName) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm does not exist");
        }
        session.getContext().setRealm(realm);
    }

    private void checkSsl(RealmModel realm) {
        if (!"https".equals(session.getContext().getUri().getBaseUri().getScheme())
                && realm.getSslRequired().isRequired(session.getContext().getConnection())) {
            HttpRequest request = session.getContext().getHttpRequest();
            Cors cors = Cors.builder().auth().allowedMethods(request.getHttpMethod()).auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST, "HTTPS required",
                    Response.Status.FORBIDDEN);
        }
    }
}
