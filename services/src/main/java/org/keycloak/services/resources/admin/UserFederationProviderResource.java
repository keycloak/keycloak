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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.mappers.FederationConfigValidationException;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.mappers.UserFederationMapperFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.UserFederationMapperRepresentation;
import org.keycloak.representations.idm.UserFederationMapperTypeRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.UsersSyncManager;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationProviderResource {

    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final RealmAuth auth;
    private final UserFederationProviderModel federationProviderModel;
    private final AdminEventBuilder adminEvent;

    @Context
    private UriInfo uriInfo;

    public UserFederationProviderResource(KeycloakSession session, RealmModel realm, RealmAuth auth, UserFederationProviderModel federationProviderModel, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.federationProviderModel = federationProviderModel;
        this.adminEvent = adminEvent.resource(ResourceType.USER_FEDERATION_PROVIDER);
    }

    /**
     * Update a provider
     *
     * @param rep
     */
    @PUT
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateProviderInstance(UserFederationProviderRepresentation rep) {
        auth.requireManage();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        String displayName = rep.getDisplayName();
        if (displayName != null && displayName.trim().equals("")) {
            displayName = null;
        }
        UserFederationProviderModel model = new UserFederationProviderModel(rep.getId(), rep.getProviderName(), rep.getConfig(), rep.getPriority(), displayName,
                rep.getFullSyncPeriod(), rep.getChangedSyncPeriod(), rep.getLastSync());

        UserFederationProvidersResource.validateFederationProviderConfig(session, auth, realm, model);

        realm.updateUserFederationProvider(model);
        new UsersSyncManager().notifyToRefreshPeriodicSync(session, realm, model, false);
        boolean kerberosCredsAdded = UserFederationProvidersResource.checkKerberosCredential(session, realm, model);
        if (kerberosCredsAdded) {
            logger.addedKerberosToRealmCredentials();
        }

        adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(rep).success();

    }

    /**
     * Get a provider
     *
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserFederationProviderRepresentation getProviderInstance() {
        auth.requireView();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        return ModelToRepresentation.toRepresentation(this.federationProviderModel);
    }

    /**
     * Delete a provider
     *
     */
    @DELETE
    @NoCache
    public void deleteProviderInstance() {
        auth.requireManage();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        realm.removeUserFederationProvider(this.federationProviderModel);
        new UsersSyncManager().notifyToRefreshPeriodicSync(session, realm, this.federationProviderModel, true);

        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();

    }

    /**
     * Trigger sync of users
     *
     * @return
     */
    @POST
    @Path("sync")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserFederationSyncResult syncUsers(@QueryParam("action") String action) {
        auth.requireManage();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        logger.debug("Syncing users");

        UsersSyncManager syncManager = new UsersSyncManager();
        UserFederationSyncResult syncResult;
        if ("triggerFullSync".equals(action)) {
            syncResult = syncManager.syncAllUsers(session.getKeycloakSessionFactory(), realm.getId(), this.federationProviderModel);
        } else if ("triggerChangedUsersSync".equals(action)) {
            syncResult = syncManager.syncChangedUsers(session.getKeycloakSessionFactory(), realm.getId(), this.federationProviderModel);
        } else {
            throw new NotFoundException("Unknown action: " + action);
        }

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("action", action);
        eventRep.put("result", syncResult);
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).representation(eventRep).success();

        return syncResult;
    }

    /**
     * Get available user federation mapper types
     *
     * @return
     */
    @GET
    @Path("mapper-types")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Map<String, UserFederationMapperTypeRepresentation> getMapperTypes() {
        auth.requireView();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        Map<String, UserFederationMapperTypeRepresentation> types = new HashMap<>();
        List<ProviderFactory> factories = sessionFactory.getProviderFactories(UserFederationMapper.class);

        for (ProviderFactory factory : factories) {
            UserFederationMapperFactory mapperFactory = (UserFederationMapperFactory)factory;
            if (mapperFactory.getFederationProviderType().equals(this.federationProviderModel.getProviderName())) {

                UserFederationMapperTypeRepresentation rep = new UserFederationMapperTypeRepresentation();
                rep.setId(mapperFactory.getId());
                rep.setCategory(mapperFactory.getDisplayCategory());
                rep.setName(mapperFactory.getDisplayType());
                rep.setHelpText(mapperFactory.getHelpText());
                rep.setSyncConfig(mapperFactory.getSyncConfig());
                List<ProviderConfigProperty> configProperties = mapperFactory.getConfigProperties();
                for (ProviderConfigProperty prop : configProperties) {
                    ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
                    propRep.setName(prop.getName());
                    propRep.setLabel(prop.getLabel());
                    propRep.setType(prop.getType());
                    propRep.setDefaultValue(prop.getDefaultValue());
                    propRep.setHelpText(prop.getHelpText());
                    rep.getProperties().add(propRep);
                }
                rep.setDefaultConfig(mapperFactory.getDefaultConfig(this.federationProviderModel));

                types.put(rep.getId(), rep);
            }
        }
        return types;
    }

    /**
     * Get mappers configured for this provider
     *
     * @return
     */
    @GET
    @Path("mappers")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<UserFederationMapperRepresentation> getMappers() {
        auth.requireView();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        List<UserFederationMapperRepresentation> mappers = new LinkedList<>();
        for (UserFederationMapperModel model : realm.getUserFederationMappersByFederationProvider(this.federationProviderModel.getId())) {
            mappers.add(ModelToRepresentation.toRepresentation(realm, model));
        }

        // Sort mappers by category,type,name
        Collections.sort(mappers, new Comparator<UserFederationMapperRepresentation>() {

            @Override
            public int compare(UserFederationMapperRepresentation o1, UserFederationMapperRepresentation o2) {
                UserFederationMapperFactory factory1 = (UserFederationMapperFactory) session.getKeycloakSessionFactory().getProviderFactory(UserFederationMapper.class, o1.getFederationMapperType());
                UserFederationMapperFactory factory2 = (UserFederationMapperFactory) session.getKeycloakSessionFactory().getProviderFactory(UserFederationMapper.class, o2.getFederationMapperType());

                int compare = factory1.getDisplayCategory().compareTo(factory2.getDisplayCategory());
                if (compare != 0) return compare;

                compare = factory1.getDisplayType().compareTo(factory2.getDisplayType());
                if (compare != 0) return compare;

                compare = o1.getName().compareTo(o2.getName());
                return compare;
            }
        });

        return mappers;
    }

    /**
     * Create a mapper
     *
     * @param mapper
     * @return
     */
    @POST
    @Path("mappers")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMapper(UserFederationMapperRepresentation mapper) {
        auth.requireManage();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        UserFederationMapperModel model = RepresentationToModel.toModel(realm, mapper);

        validateModel(model);

        model = realm.addUserFederationMapper(model);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.USER_FEDERATION_MAPPER).resourcePath(uriInfo, model.getId())
                .representation(mapper).success();

        return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();

    }

    /**
     * Get a mapper
     *
     * @param id Mapper id
     * @return
     */
    @GET
    @NoCache
    @Path("mappers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserFederationMapperRepresentation getMapperById(@PathParam("id") String id) {
        auth.requireView();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        UserFederationMapperModel model = realm.getUserFederationMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        return ModelToRepresentation.toRepresentation(realm, model);
    }

    /**
     * Update a mapper
     *
     * @param id Mapper id
     * @param rep
     */
    @PUT
    @NoCache
    @Path("mappers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("id") String id, UserFederationMapperRepresentation rep) {
        auth.requireManage();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        UserFederationMapperModel model = realm.getUserFederationMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        model = RepresentationToModel.toModel(realm, rep);

        validateModel(model);

        realm.updateUserFederationMapper(model);
        adminEvent.operation(OperationType.UPDATE).resource(ResourceType.USER_FEDERATION_MAPPER).resourcePath(uriInfo).representation(rep).success();

    }

    /**
     * Delete a mapper with a given id
     *
     * @param id Mapper id
     */
    @DELETE
    @NoCache
    @Path("mappers/{id}")
    public void delete(@PathParam("id") String id) {
        auth.requireManage();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        UserFederationMapperModel model = realm.getUserFederationMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        realm.removeUserFederationMapper(model);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.USER_FEDERATION_MAPPER).resourcePath(uriInfo).success();

    }

    /**
     * Trigger sync of mapper data related to federationMapper (roles, groups, ...)
     *
     * @return
     */
    @POST
    @Path("mappers/{id}/sync")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserFederationSyncResult syncMapperData(@PathParam("id") String mapperId, @QueryParam("direction") String direction) {
        auth.requireManage();

        if (federationProviderModel == null) {
            throw new NotFoundException("Could not find federation provider");
        }

        UserFederationMapperModel mapperModel = realm.getUserFederationMapperById(mapperId);
        if (mapperModel == null) throw new NotFoundException("Mapper model not found");
        UserFederationMapper mapper = session.getProvider(UserFederationMapper.class, mapperModel.getFederationMapperType());

        UserFederationProviderModel providerModel = KeycloakModelUtils.findUserFederationProviderById(mapperModel.getFederationProviderId(), realm);
        if (providerModel == null) throw new NotFoundException("Provider model not found");
        UserFederationProviderFactory providerFactory = (UserFederationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, providerModel.getProviderName());
        UserFederationProvider federationProvider = providerFactory.getInstance(session, providerModel);

        logger.syncingDataForMapper(mapperModel.getName(), mapperModel.getFederationMapperType(), direction);

        UserFederationSyncResult syncResult;
        if ("fedToKeycloak".equals(direction)) {
            syncResult = mapper.syncDataFromFederationProviderToKeycloak(mapperModel, federationProvider, session, realm);
        } else if ("keycloakToFed".equals(direction)) {
            syncResult = mapper.syncDataFromKeycloakToFederationProvider(mapperModel, federationProvider, session, realm);
        } else {
            throw new NotFoundException("Unknown direction: " + direction);
        }

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("action", direction);
        eventRep.put("result", syncResult);
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).representation(eventRep).success();
        return syncResult;
    }

    private void validateModel(UserFederationMapperModel model) {
        try {
            UserFederationMapperFactory mapperFactory = (UserFederationMapperFactory) session.getKeycloakSessionFactory().getProviderFactory(UserFederationMapper.class, model.getFederationMapperType());
            mapperFactory.validateConfig(realm, federationProviderModel, model);
        } catch (FederationConfigValidationException ex) {
            logger.error(ex.getMessage());
            Properties messages = AdminRoot.getMessages(session, realm, auth.getAuth().getToken().getLocale());
            throw new ErrorResponseException(ex.getMessage(), MessageFormat.format(messages.getProperty(ex.getMessage(), ex.getMessage()), ex.getParameters()),
                    Response.Status.BAD_REQUEST);
        }
    }

}
