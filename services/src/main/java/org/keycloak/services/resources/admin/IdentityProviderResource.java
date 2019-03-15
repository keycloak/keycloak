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

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import javax.ws.rs.NotFoundException;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @resource Identity Providers
 * @author Pedro Igor
 */
public class IdentityProviderResource {

    protected static final Logger logger = Logger.getLogger(IdentityProviderResource.class);

    private final AdminPermissionEvaluator auth;
    private final RealmModel realm;
    private final KeycloakSession session;
    private final IdentityProviderModel identityProviderModel;
    private final AdminEventBuilder adminEvent;

    public IdentityProviderResource(AdminPermissionEvaluator auth, RealmModel realm, KeycloakSession session, IdentityProviderModel identityProviderModel, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.identityProviderModel = identityProviderModel;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.IDENTITY_PROVIDER);
    }

    /**
     * Get the identity provider
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityProviderRepresentation getIdentityProvider() {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        IdentityProviderRepresentation rep = ModelToRepresentation.toRepresentation(realm, this.identityProviderModel);
        return StripSecretsUtils.strip(rep);
    }

    /**
     * Delete the identity provider
     *
     * @return
     */
    @DELETE
    @NoCache
    public Response delete() {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        String alias = this.identityProviderModel.getAlias();
        this.realm.removeIdentityProviderByAlias(alias);

        Set<IdentityProviderMapperModel> mappers = this.realm.getIdentityProviderMappersByAlias(alias);
        for (IdentityProviderMapperModel mapper : mappers) {
            this.realm.removeIdentityProviderMapper(mapper);
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();

        return Response.noContent().build();
    }

    /**
     * Update the identity provider
     *
     * @param providerRep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response update(IdentityProviderRepresentation providerRep) {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        try {
            updateIdpFromRep(providerRep, realm, session);

            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(providerRep).success();

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Identity Provider " + providerRep.getAlias() + " already exists");
        }
    }

    private void updateIdpFromRep(IdentityProviderRepresentation providerRep, RealmModel realm, KeycloakSession session) {
        String internalId = providerRep.getInternalId();
        String newProviderId = providerRep.getAlias();
        String oldProviderId = getProviderIdByInternalId(realm, internalId);

        if (oldProviderId == null) {
            lookUpProviderIdByAlias(realm, providerRep);
        }

        IdentityProviderModel updated = RepresentationToModel.toModel(realm, providerRep);

        if (updated.getConfig() != null && ComponentRepresentation.SECRET_VALUE.equals(updated.getConfig().get("clientSecret"))) {
            updated.getConfig().put("clientSecret", identityProviderModel.getConfig() != null ? identityProviderModel.getConfig().get("clientSecret") : null);
        }

        realm.updateIdentityProvider(updated);

        if (oldProviderId != null && !oldProviderId.equals(newProviderId)) {

            // Admin changed the ID (alias) of identity provider. We must update all clients and users
            logger.debug("Changing providerId in all clients and linked users. oldProviderId=" + oldProviderId + ", newProviderId=" + newProviderId);

            updateUsersAfterProviderAliasChange(session.users().getUsers(realm, false), oldProviderId, newProviderId, realm, session);
        }
    }

    // return ID of IdentityProvider from realm based on internalId of this provider
    private static String getProviderIdByInternalId(RealmModel realm, String providerInternalId) {
        List<IdentityProviderModel> providerModels = realm.getIdentityProviders();
        for (IdentityProviderModel providerModel : providerModels) {
            if (providerModel.getInternalId().equals(providerInternalId)) {
                return providerModel.getAlias();
            }
        }

        return null;
    }

    // sets internalId to IdentityProvider based on alias
    private static void lookUpProviderIdByAlias(RealmModel realm, IdentityProviderRepresentation providerRep) {
        List<IdentityProviderModel> providerModels = realm.getIdentityProviders();
        for (IdentityProviderModel providerModel : providerModels) {
            if (providerModel.getAlias().equals(providerRep.getAlias())) {
                providerRep.setInternalId(providerModel.getInternalId());
                return;
            }
        }
        throw new javax.ws.rs.NotFoundException();
    }

    private static void updateUsersAfterProviderAliasChange(List<UserModel> users, String oldProviderId, String newProviderId, RealmModel realm, KeycloakSession session) {
        for (UserModel user : users) {
            FederatedIdentityModel federatedIdentity = session.users().getFederatedIdentity(user, oldProviderId, realm);
            if (federatedIdentity != null) {
                // Remove old link first
                session.users().removeFederatedIdentity(realm, user, oldProviderId);

                // And create new
                FederatedIdentityModel newFederatedIdentity = new FederatedIdentityModel(newProviderId, federatedIdentity.getUserId(), federatedIdentity.getUserName(),
                        federatedIdentity.getToken());
                session.users().addFederatedIdentity(realm, user, newFederatedIdentity);
            }
        }
    }


    private IdentityProviderFactory getIdentityProviderFactory() {
        List<ProviderFactory> allProviders = new ArrayList<ProviderFactory>();

        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(IdentityProvider.class));
        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class));

        for (ProviderFactory providerFactory : allProviders) {
            if (providerFactory.getId().equals(identityProviderModel.getProviderId())) return (IdentityProviderFactory)providerFactory;
        }

        return null;
    }

    /**
     * Export public broker configuration for identity provider
     *
     * @param format Format to use
     * @return
     */
    @GET
    @Path("export")
    @NoCache
    public Response export(@QueryParam("format") String format) {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        try {
            IdentityProviderFactory factory = getIdentityProviderFactory();
            return factory.create(session, identityProviderModel).export(session.getContext().getUri(), realm, format);
        } catch (Exception e) {
            return ErrorResponse.error("Could not export public broker configuration for identity provider [" + identityProviderModel.getProviderId() + "].", Response.Status.NOT_FOUND);
        }
    }

    /**
     * Get mapper types for identity provider
     */
    @GET
    @Path("mapper-types")
    @NoCache
    public Map<String, IdentityProviderMapperTypeRepresentation> getMapperTypes() {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        Map<String, IdentityProviderMapperTypeRepresentation> types = new HashMap<>();
        List<ProviderFactory> factories = sessionFactory.getProviderFactories(IdentityProviderMapper.class);
        for (ProviderFactory factory : factories) {
            IdentityProviderMapper mapper = (IdentityProviderMapper)factory;
            for (String type : mapper.getCompatibleProviders()) {
                if (IdentityProviderMapper.ANY_PROVIDER.equals(type) || type.equals(identityProviderModel.getProviderId())) {
                    IdentityProviderMapperTypeRepresentation rep = new IdentityProviderMapperTypeRepresentation();
                    rep.setId(mapper.getId());
                    rep.setCategory(mapper.getDisplayCategory());
                    rep.setName(mapper.getDisplayType());
                    rep.setHelpText(mapper.getHelpText());
                    List<ProviderConfigProperty> configProperties = mapper.getConfigProperties();
                    for (ProviderConfigProperty prop : configProperties) {
                        ConfigPropertyRepresentation propRep = ModelToRepresentation.toRepresentation(prop);
                        rep.getProperties().add(propRep);
                    }
                    types.put(rep.getId(), rep);
                    break;
                }
            }
        }
        return types;
    }

    /**
     * Get mappers for identity provider
     */
    @GET
    @Path("mappers")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<IdentityProviderMapperRepresentation> getMappers() {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        List<IdentityProviderMapperRepresentation> mappers = new LinkedList<>();
        for (IdentityProviderMapperModel model : realm.getIdentityProviderMappersByAlias(identityProviderModel.getAlias())) {
            mappers.add(ModelToRepresentation.toRepresentation(model));
        }
        return mappers;
    }

    /**
     * Add a mapper to identity provider
     *
     * @param mapper
     * @return
     */
    @POST
    @Path("mappers")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMapper(IdentityProviderMapperRepresentation mapper) {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = RepresentationToModel.toModel(mapper);
        try {
            model = realm.addIdentityProviderMapper(model);
        } catch (Exception e) {
            return ErrorResponse.error("Failed to add mapper '" + model.getName() + "' to identity provider [" + identityProviderModel.getProviderId() + "].", Response.Status.BAD_REQUEST);
        }

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.IDENTITY_PROVIDER_MAPPER).resourcePath(session.getContext().getUri(), model.getId())
            .representation(mapper).success();

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();

    }

    /**
     * Get mapper by id for the identity provider
     *
     * @param id
     * @return
     */
    @GET
    @NoCache
    @Path("mappers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityProviderMapperRepresentation getMapperById(@PathParam("id") String id) {
        this.auth.realm().requireViewIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = realm.getIdentityProviderMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Update a mapper for the identity provider
     *
     * @param id Mapper id
     * @param rep
     */
    @PUT
    @NoCache
    @Path("mappers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("id") String id, IdentityProviderMapperRepresentation rep) {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = realm.getIdentityProviderMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        model = RepresentationToModel.toModel(rep);
        realm.updateIdentityProviderMapper(model);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.IDENTITY_PROVIDER_MAPPER).resourcePath(session.getContext().getUri()).representation(rep).success();

    }

    /**
     * Delete a mapper for the identity provider
     *
     * @param id Mapper id
     */
    @DELETE
    @NoCache
    @Path("mappers/{id}")
    public void delete(@PathParam("id") String id) {
        this.auth.realm().requireManageIdentityProviders();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = realm.getIdentityProviderMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        realm.removeIdentityProviderMapper(model);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.IDENTITY_PROVIDER_MAPPER).resourcePath(session.getContext().getUri()).success();

    }

    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     * @return
     */
    @Path("management/permissions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference getManagementPermissions() {
        this.auth.realm().requireViewIdentityProviders();

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.idps().isPermissionsEnabled(identityProviderModel)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(identityProviderModel, permissions);
    }

    public static ManagementPermissionReference toMgmtRef(IdentityProviderModel model, AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.idps().resource(model).getId());
        ref.setScopePermissions(permissions.idps().getPermissions(model));
        return ref;
    }


    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     *
     * @return initialized manage permissions reference
     */
    @Path("management/permissions")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference setManagementPermissionsEnabled(ManagementPermissionReference ref) {
        this.auth.realm().requireManageIdentityProviders();
        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.idps().setPermissionsEnabled(identityProviderModel, ref.isEnabled());
        if (ref.isEnabled()) {
            return toMgmtRef(identityProviderModel, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }





}
