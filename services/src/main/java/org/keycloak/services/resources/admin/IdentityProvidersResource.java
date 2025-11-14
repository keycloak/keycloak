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

package org.keycloak.services.resources.admin;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.http.FormPartValue;
import org.keycloak.models.IdentityProviderCapability;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.IdentityProviderType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.StringUtil;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @resource Identity Providers
 * @author Pedro Igor
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class IdentityProvidersResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public IdentityProvidersResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.IDENTITY_PROVIDER);
    }

    /**
     * Get the identity provider factory for a provider id.
     *
     * @param providerId Provider id
     * @return
     */
    @Path("/providers/{provider_id}")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Get the identity provider factory for that provider id")
    public IdentityProviderFactory getIdentityProviderFactory(@Parameter(description = "The provider id to get the factory") @PathParam("provider_id") String providerId) {
        this.auth.realm().requireViewIdentityProviders();
        IdentityProviderFactory providerFactory = getProviderFactoryById(providerId);
        if (providerFactory != null) {
            return providerFactory;
        }
        throw new BadRequestException();
    }

    /**
     * Import identity provider from uploaded JSON file
     */
    @POST
    @Path("import-config")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( description = "Import identity provider from uploaded JSON file")
    public Map<String, String> importFrom() throws IOException {
        this.auth.realm().requireManageIdentityProviders();
        MultivaluedMap<String, FormPartValue> formDataMap = session.getContext().getHttpRequest().getMultiPartFormParameters();
        if (!(formDataMap.containsKey("providerId") && formDataMap.containsKey("file"))) {
            throw new BadRequestException();
        }
        String providerId = formDataMap.getFirst("providerId").asString();
        String config = StreamUtil.readString(formDataMap.getFirst("file").asInputStream());
        IdentityProviderFactory<?> providerFactory = getProviderFactoryById(providerId);
        return providerFactory.parseConfig(session, config);
    }

    /**
     * Import identity provider from JSON body
     *
     * @param data JSON body
     * @return
     * @throws IOException
     */
    @POST
    @Path("import-config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Import identity provider from JSON body")
    public Map<String, String> importFrom(@Parameter(description = "JSON body") Map<String, Object> data) throws IOException {
        this.auth.realm().requireManageIdentityProviders();
        if (data == null || !(data.containsKey("providerId") && data.containsKey("fromUrl"))) {
            throw new BadRequestException();
        }

        ReservedCharValidator.validateNoSpace((String)data.get("alias"));

        String providerId = data.get("providerId").toString();
        String from = data.get("fromUrl").toString();
        String file = session.getProvider(HttpClientProvider.class).getString(from);
        IdentityProviderFactory providerFactory = getProviderFactoryById(providerId);
        Map<String, String> config = providerFactory.parseConfig(session, file);
        // add the URL just if needed by the identity provider
        config.put(IdentityProviderModel.METADATA_DESCRIPTOR_URL, from);
        return config;
    }

    /**
     * List identity providers.
     *
     * @param search Filter to search specific providers by name. Search can be prefixed (name*), contains (*name*) or exact (\"name\"). Default prefixed.
     * @param briefRepresentation Boolean which defines whether brief representations are returned (default: false)
     * @param firstResult Pagination offset
     * @param maxResults Maximum results size (defaults to 100)
     * @return The list of providers.
     */
    @GET
    @Path("instances")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation(summary = "List identity providers")
    public Stream<IdentityProviderRepresentation> getIdentityProviders(
            @Parameter(description = "Filter by identity providers type") @QueryParam("type") String type,
            @Parameter(description = "Filter by identity providers capability") @QueryParam("capability") String capability,
            @Parameter(description = "Filter specific providers by name. Search can be prefix (name*), contains (*name*) or exact (\"name\"). Default prefixed.") @QueryParam("search") String search,
            @Parameter(description = "Boolean which defines whether brief representations are returned (default: false)") @QueryParam("briefRepresentation") Boolean briefRepresentation,
            @Parameter(description = "Pagination offset") @QueryParam("first") Integer firstResult,
            @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults,
            @Parameter(description = "Boolean which defines if only realm-level IDPs (not associated with orgs) should be returned (default: false)") @QueryParam("realmOnly") Boolean realmOnly) {
        this.auth.realm().requireViewIdentityProviders();

        if (maxResults == null) {
            maxResults = 100; // always set a maximum of 100 by default
        }

        Function<IdentityProviderModel, IdentityProviderRepresentation> toRepresentation = Optional.ofNullable(briefRepresentation).orElse(false)
                ? m -> ModelToRepresentation.toBriefRepresentation(realm, m)
                : m -> StripSecretsUtils.stripSecrets(session, ModelToRepresentation.toRepresentation(session, realm, m));

        boolean searchRealmOnlyIDPs = Optional.ofNullable(realmOnly).orElse(false);

        IdentityProviderQuery query;
        if (type != null) {
            query = IdentityProviderQuery.type(IdentityProviderType.valueOf(type));
        } else if (capability != null) {
            query = IdentityProviderQuery.capability(IdentityProviderCapability.valueOf(capability));
        } else {
            query = IdentityProviderQuery.any();
        }

        if (StringUtil.isNotBlank(search)) {
            query.with(IdentityProviderModel.SEARCH, search);
        }
        if (searchRealmOnlyIDPs) {
            query.with(IdentityProviderModel.ORGANIZATION_ID, null);
        }

        return session.identityProviders().getAllStream(query, firstResult, maxResults).map(toRepresentation);
    }

    /**
     * Create a new identity provider
     *
     * @param representation JSON body
     * @return
     */
    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.IDENTITY_PROVIDERS)
    @Operation( summary = "Create a new identity provider")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response create(@Parameter(description = "JSON body") IdentityProviderRepresentation representation) {
        this.auth.realm().requireManageIdentityProviders();

        ReservedCharValidator.validateNoSpace(representation.getAlias());

        try {
            IdentityProviderModel identityProvider = RepresentationToModel.toModel(realm, representation, session);
            session.identityProviders().create(identityProvider);

            representation.setInternalId(identityProvider.getInternalId());
            representation.setHideOnLogin(identityProvider.isHideOnLogin()); // update in case of legacy hide on login attr was used.
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), identityProvider.getAlias())
                    .representation(StripSecretsUtils.stripSecrets(session, representation)).success();

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(representation.getAlias()).build()).build();
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();

            if (message == null) {
                message = "Invalid request";
            }

            throw ErrorResponse.error(message, BAD_REQUEST);
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Identity Provider " + representation.getAlias() + " already exists");
        }
    }

    @Path("instances/{alias}")
    public IdentityProviderResource getIdentityProvider(@PathParam("alias") String alias) {
        this.auth.realm().requireViewIdentityProviders();
        IdentityProviderModel identityProviderModel = session.identityProviders().getByIdOrAlias(alias);

        return new IdentityProviderResource(this.auth, realm, session, identityProviderModel, adminEvent);
    }

    private IdentityProviderFactory<?> getProviderFactoryById(String providerId) {
        return getProviderFactories()
                .filter(providerFactory -> Objects.equals(providerId, providerFactory.getId()))
                .map(IdentityProviderFactory.class::cast)
                .findFirst()
                .orElse(null);
    }

    private Stream<ProviderFactory> getProviderFactories() {
        return Stream.concat(session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class));
    }
}
