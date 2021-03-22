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

import static org.keycloak.protocol.ProtocolMapperUtils.isEnabled;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import javax.ws.rs.NotFoundException;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Base resource for managing users
 *
 * @resource Protocol Mappers
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMappersResource {
    protected static final Logger logger = Logger.getLogger(ProtocolMappersResource.class);

    protected RealmModel realm;

    protected ProtocolMapperContainerModel client;

    protected AdminPermissionEvaluator auth;
    protected AdminPermissionEvaluator.RequirePermissionCheck managePermission;
    protected AdminPermissionEvaluator.RequirePermissionCheck viewPermission;

    protected AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ProtocolMappersResource(RealmModel realm, ProtocolMapperContainerModel client, AdminPermissionEvaluator auth,
                                   AdminEventBuilder adminEvent,
                                   AdminPermissionEvaluator.RequirePermissionCheck managePermission,
                                   AdminPermissionEvaluator.RequirePermissionCheck viewPermission) {
        this.realm = realm;
        this.auth = auth;
        this.client = client;
        this.adminEvent = adminEvent.resource(ResourceType.PROTOCOL_MAPPER);
        this.managePermission = managePermission;
        this.viewPermission = viewPermission;

    }

    /**
     * Get mappers by name for a specific protocol
     *
     * @param protocol
     * @return
     */
    @GET
    @NoCache
    @Path("protocol/{protocol}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProtocolMapperRepresentation> getMappersPerProtocol(@PathParam("protocol") String protocol) {
        viewPermission.require();

        List<ProtocolMapperRepresentation> mappers = new LinkedList<ProtocolMapperRepresentation>();
        for (ProtocolMapperModel mapper : client.getProtocolMappers()) {
            if (isEnabled(session, mapper) && mapper.getProtocol().equals(protocol)) mappers.add(ModelToRepresentation.toRepresentation(mapper));
        }
        return mappers;
    }

    /**
     * Create a mapper
     *
     * @param rep
     */
    @Path("models")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createMapper(ProtocolMapperRepresentation rep) {
        managePermission.require();

        ProtocolMapperModel model = null;
        try {
            model = RepresentationToModel.toModel(rep);
            validateModel(model);
            model = client.addProtocolMapper(model);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), model.getId()).representation(rep).success();

        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Protocol mapper exists with same name");
        }

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
    }
    /**
     * Create multiple mappers
     *
     */
    @Path("add-models")
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public void createMapper(List<ProtocolMapperRepresentation> reps) {
        managePermission.require();

        ProtocolMapperModel model = null;
        for (ProtocolMapperRepresentation rep : reps) {
            model = RepresentationToModel.toModel(rep);
            validateModel(model);
            model = client.addProtocolMapper(model);
        }
        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(reps).success();
    }

    /**
     * Get mappers
     *
     * @return
     */
    @GET
    @NoCache
    @Path("models")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProtocolMapperRepresentation> getMappers() {
        viewPermission.require();

        List<ProtocolMapperRepresentation> mappers = new LinkedList<ProtocolMapperRepresentation>();
        for (ProtocolMapperModel mapper : client.getProtocolMappers()) {
            if (isEnabled(session, mapper)) {
                mappers.add(ModelToRepresentation.toRepresentation(mapper));
            }
        }
        return mappers;
    }

    /**
     * Get mapper by id
     *
     * @param id Mapper id
     * @return
     */
    @GET
    @NoCache
    @Path("models/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProtocolMapperRepresentation getMapperById(@PathParam("id") String id) {
        viewPermission.require();

        ProtocolMapperModel model = client.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Update the mapper
     *
     * @param id Mapper id
     * @param rep
     */
    @PUT
    @NoCache
    @Path("models/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("id") String id, ProtocolMapperRepresentation rep) {
        managePermission.require();

        ProtocolMapperModel model = client.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        model = RepresentationToModel.toModel(rep);

        validateModel(model);

        client.updateProtocolMapper(model);
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
    }

    /**
     * Delete the mapper
     *
     * @param id Mapper id
     */
    @DELETE
    @NoCache
    @Path("models/{id}")
    public void delete(@PathParam("id") String id) {
        managePermission.require();

        ProtocolMapperModel model = client.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        client.removeProtocolMapper(model);
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();

    }

    private void validateModel(ProtocolMapperModel model) {
        try {
            ProtocolMapper mapper = (ProtocolMapper)session.getKeycloakSessionFactory().getProviderFactory(ProtocolMapper.class, model.getProtocolMapper());
            if (mapper != null) {
                mapper.validateConfig(session, realm, client, model);
            } else {
                throw new NotFoundException("ProtocolMapper provider not found");
            }
        } catch (ProtocolMapperConfigException ex) {
            logger.error(ex.getMessage());
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(ex.getMessage(), MessageFormat.format(messages.getProperty(ex.getMessageKey(), ex.getMessage()), ex.getParameters()),
                    Response.Status.BAD_REQUEST);
        }
    }

}
