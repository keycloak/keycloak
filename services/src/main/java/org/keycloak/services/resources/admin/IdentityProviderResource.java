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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
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
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.broker.social.SocialIdentityProvider;

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

/**
 * @author Pedro Igor
 */
public class IdentityProviderResource {

    private static ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private final RealmAuth auth;
    private final RealmModel realm;
    private final KeycloakSession session;
    private final IdentityProviderModel identityProviderModel;
    private final AdminEventBuilder adminEvent;

    @Context private UriInfo uriInfo;

    public IdentityProviderResource(RealmAuth auth, RealmModel realm, KeycloakSession session, IdentityProviderModel identityProviderModel, AdminEventBuilder adminEvent) {
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
        this.auth.requireView();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        IdentityProviderRepresentation rep = ModelToRepresentation.toRepresentation(realm, this.identityProviderModel);
        return rep;
    }

    /**
     * Delete the identity provider
     *
     * @return
     */
    @DELETE
    @NoCache
    public Response delete() {
        this.auth.requireManage();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        this.realm.removeIdentityProviderByAlias(this.identityProviderModel.getAlias());

        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();

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
        this.auth.requireManage();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        try {
            updateIdpFromRep(providerRep, realm, session);

            adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(providerRep).success();

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Identity Provider " + providerRep.getAlias() + " already exists");
        }
    }

    public static void updateIdpFromRep(IdentityProviderRepresentation providerRep, RealmModel realm, KeycloakSession session) {
        String internalId = providerRep.getInternalId();
        String newProviderId = providerRep.getAlias();
        String oldProviderId = getProviderIdByInternalId(realm, internalId);

        realm.updateIdentityProvider(RepresentationToModel.toModel(realm, providerRep));

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
     * @param uriInfo
     * @param format Format to use
     * @return
     */
    @GET
    @Path("export")
    @NoCache
    public Response export(@Context UriInfo uriInfo, @QueryParam("format") String format) {
        this.auth.requireView();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        try {
            IdentityProviderFactory factory = getIdentityProviderFactory();
            return factory.create(identityProviderModel).export(uriInfo, realm, format);
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
        this.auth.requireView();

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
                        ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
                        propRep.setName(prop.getName());
                        propRep.setLabel(prop.getLabel());
                        propRep.setType(prop.getType());
                        propRep.setDefaultValue(prop.getDefaultValue());
                        propRep.setHelpText(prop.getHelpText());
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
        this.auth.requireView();

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
        auth.requireManage();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = RepresentationToModel.toModel(mapper);
        model = realm.addIdentityProviderMapper(model);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.IDENTITY_PROVIDER_MAPPER).resourcePath(uriInfo, model.getId())
            .representation(mapper).success();

        return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();

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
        auth.requireView();

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
        auth.requireManage();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = realm.getIdentityProviderMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        model = RepresentationToModel.toModel(rep);
        realm.updateIdentityProviderMapper(model);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.IDENTITY_PROVIDER_MAPPER).resourcePath(uriInfo).representation(rep).success();

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
        auth.requireManage();

        if (identityProviderModel == null) {
            throw new javax.ws.rs.NotFoundException();
        }

        IdentityProviderMapperModel model = realm.getIdentityProviderMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        realm.removeIdentityProviderMapper(model);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.IDENTITY_PROVIDER_MAPPER).resourcePath(uriInfo).success();

    }


}
