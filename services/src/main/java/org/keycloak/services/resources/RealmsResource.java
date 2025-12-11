/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import java.net.URI;
import java.util.Comparator;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationService;
import org.keycloak.common.Profile;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientregistration.ClientRegistrationService;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.account.AccountLoader;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.utils.ProfileHelper;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

import org.jboss.logging.Logger;

import static org.keycloak.utils.MediaType.APPLICATION_JWT;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
@Path("/realms")
public class RealmsResource {
    protected static final Logger logger = Logger.getLogger(RealmsResource.class);

    @Context
    protected KeycloakSession session;

    public static UriBuilder realmBaseUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return realmBaseUrl(baseUriBuilder);
    }

    public static UriBuilder realmBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path(RealmsResource.class, "getRealmResource");
    }

    public static UriBuilder accountUrl(UriBuilder base) {
        return base.path(RealmsResource.class).path(RealmsResource.class, "getAccountService");
    }

    public static UriBuilder protocolUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getProtocol");
    }

    public static UriBuilder protocolUrl(UriBuilder builder) {
        return builder.path(RealmsResource.class).path(RealmsResource.class, "getProtocol");
    }

    public static UriBuilder clientRegistrationUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getClientsService");
    }

    public static UriBuilder brokerUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getBrokerService");
    }

    public static UriBuilder wellKnownProviderUrl(UriBuilder builder) {
        return builder.path(RealmsResource.class).path(RealmsResource.class, "getWellKnown");
    }

    @Path("{realm}/protocol/{protocol}")
    public Object getProtocol(final @PathParam("realm") String name,
                              final @PathParam("protocol") String protocol) {
        resolveRealmAndUpdateSession(name);

        LoginProtocolFactory factory = (LoginProtocolFactory)session.getKeycloakSessionFactory().getProviderFactory(LoginProtocol.class, protocol);
        if(factory == null){
            logger.debugf("protocol %s not found", protocol);
            throw new NotFoundException("Protocol not found");
        }

        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());

        return factory.createProtocolEndpoint(session, event);
    }

    /**
     * Returns a temporary redirect to the client url configured for the given {@code clientId} in the given {@code realmName}.
     * <p>
     * This allows a client to refer to other clients just by their client id in URLs, will then redirect users to the actual client url.
     * The client url is derived according to the rules of the base url in the client configuration.
     * </p>
     *
     * @param realmName
     * @param clientId
     * @return
     * @since 2.0
     */
    @GET
    @Path("{realm}/clients/{client_id}/redirect")
    public Response getRedirect(final @PathParam("realm") String realmName, final @PathParam("client_id") String clientId) {
        resolveRealmAndUpdateSession(realmName);

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = realm.getClientByClientId(clientId);

        if (client == null) {
            return null;
        }

        if (client.getRootUrl() == null && client.getBaseUrl() == null) {
            return null;
        }


        URI targetUri;
        if (client.getRootUrl() != null && (client.getBaseUrl() == null || client.getBaseUrl().isEmpty())) {
            targetUri = KeycloakUriBuilder.fromUri(client.getRootUrl()).build();
        } else {
            targetUri = KeycloakUriBuilder.fromUri(ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), client.getBaseUrl())).build();
        }

        return Response.seeOther(targetUri).build();
    }

    @Path("{realm}/login-actions")
    public LoginActionsService getLoginActionsService(final @PathParam("realm") String name) {
        resolveRealmAndUpdateSession(name);
        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
        return new LoginActionsService(session, event);
    }

    @Path("{realm}/clients-registrations")
    public ClientRegistrationService getClientsService(final @PathParam("realm") String name) {
        resolveRealmAndUpdateSession(name);
        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
        return new ClientRegistrationService(session, event);
    }

    @Path("{realm}/clients-managements")
    public ClientsManagementService getClientsManagementService(final @PathParam("realm") String name) {
        resolveRealmAndUpdateSession(name);
        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
        return new ClientsManagementService(session, event);
    }

    private void resolveRealmAndUpdateSession(String realmName) {
        resolveRealmAndUpdateSession(session, realmName);
    }

    private static void resolveRealmAndUpdateSession(KeycloakSession session, String realmName) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm does not exist");
        }
        session.getContext().setRealm(realm);
    }

    @Path("{realm}/account")
    public Object getAccountService(final @PathParam("realm") String name) {
        resolveRealmAndUpdateSession(name);
        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());
        return new AccountLoader(session, event);
    }

    @Path("{realm}")
    public PublicRealmResource getRealmResource(final @PathParam("realm") String name) {
        resolveRealmAndUpdateSession(name);
        return new PublicRealmResource(session);
    }

    @Path("{realm}/broker")
    public IdentityBrokerService getBrokerService(final @PathParam("realm") String name) {
        resolveRealmAndUpdateSession(name);

        IdentityBrokerService brokerService = new IdentityBrokerService(session);

        brokerService.init();

        return brokerService;
    }

    @OPTIONS
    @Path("{realm}/.well-known/{provider}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersionPreflight(final @PathParam("realm") String name,
                                        final @PathParam("provider") String providerName) {
        return Cors.builder().allowedMethods("GET").preflight().auth().add(Response.ok());
    }

    @GET
    @Path("{realm}/.well-known/{alias}")
    @Produces({MediaType.APPLICATION_JSON, APPLICATION_JWT})
    public Response getWellKnown(final @PathParam("realm") String name,
                                 final @PathParam("alias") String alias) {
        return getWellKnownResponse(session, name, alias, logger);
    }

    public static Response getWellKnownResponse(KeycloakSession session, String name, String alias, Logger logger) throws NotFoundException {
        resolveRealmAndUpdateSession(session, name);
        checkSsl(session, session.getContext().getRealm());

        WellKnownProviderFactory wellKnownProviderFactoryFound = session.getKeycloakSessionFactory().getProviderFactoriesStream(WellKnownProvider.class)
                .map(providerFactory -> (WellKnownProviderFactory) providerFactory)
                .filter(wellKnownProviderFactory -> alias.equals(wellKnownProviderFactory.getAlias()))
                .sorted(Comparator.comparingInt(WellKnownProviderFactory::getPriority))
                .findFirst().orElseThrow(NotFoundException::new);

        logger.tracef("Use provider with ID '%s' for well-known alias '%s'", wellKnownProviderFactoryFound.getId(), alias);

        WellKnownProvider wellKnown = session.getProvider(WellKnownProvider.class, wellKnownProviderFactoryFound.getId());

        if (wellKnown != null) {
            Object config = wellKnown.getConfig();
            Response.ResponseBuilder responseBuilder;

            // Check if the provider returned a JWT string or JSON object
            responseBuilder = Response.ok(config).type(config instanceof String ? APPLICATION_JWT : MediaType.APPLICATION_JSON);

            return Cors.builder().allowAllOrigins().auth().add(responseBuilder.cacheControl(CacheControlUtil.noCache()));
        }

        throw new NotFoundException();
    }

    @Path("{realm}/authz")
    public Object getAuthorizationService(@PathParam("realm") String name) {
        ProfileHelper.requireFeature(Profile.Feature.AUTHORIZATION);

        resolveRealmAndUpdateSession(name);
        AuthorizationProvider authorization = this.session.getProvider(AuthorizationProvider.class);
        return new AuthorizationService(authorization);
    }

    /**
     * A JAX-RS sub-resource locator that uses the {@link org.keycloak.services.resource.RealmResourceSPI} to resolve sub-resources instances given an <code>unknownPath</code>.
     *
     * @param extension a path that could be to a REST extension
     * @return a JAX-RS sub-resource instance for the REST extension if found. Otherwise null is returned.
     */
    @Path("{realm}/{extension}")
    public Object resolveRealmExtension(@PathParam("realm") String realmName, @PathParam("extension") String extension) {
        resolveRealmAndUpdateSession(realmName);
        RealmResourceProvider provider = session.getProvider(RealmResourceProvider.class, extension);
        if (provider != null) {
            Object resource = provider.getResource();
            if (resource != null) {
                return resource;
            }
        }

        throw new NotFoundException();
    }

    private void checkSsl(RealmModel realm) {
        checkSsl(session, realm);
    }

    private static void checkSsl(KeycloakSession session, RealmModel realm) {
        if (!"https".equals(session.getContext().getUri().getBaseUri().getScheme())
                && realm.getSslRequired().isRequired(session.getContext().getConnection())) {
            HttpRequest request = session.getContext().getHttpRequest();
            Cors cors = Cors.builder().auth().allowedMethods(request.getHttpMethod()).auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST, "HTTPS required",
                    Response.Status.FORBIDDEN);
        }
    }
}
