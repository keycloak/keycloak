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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import jakarta.ws.rs.NotFoundException;
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
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Base resource for managing users
 *
 * @resource Protocol Mappers
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ProtocolMappersResource {
    protected static final Logger logger = Logger.getLogger(ProtocolMappersResource.class);

    protected final RealmModel realm;

    protected final ProtocolMapperContainerModel client;

    protected final AdminPermissionEvaluator auth;
    protected final AdminPermissionEvaluator.RequirePermissionCheck managePermission;
    protected final AdminPermissionEvaluator.RequirePermissionCheck viewPermission;

    protected final AdminEventBuilder adminEvent;

    protected final KeycloakSession session;

    public ProtocolMappersResource(KeycloakSession session, ProtocolMapperContainerModel client, AdminPermissionEvaluator auth,
                                   AdminEventBuilder adminEvent,
                                   AdminPermissionEvaluator.RequirePermissionCheck managePermission,
                                   AdminPermissionEvaluator.RequirePermissionCheck viewPermission) {
        this.session = session;
        this.realm = session.getContext().getRealm();
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.PROTOCOL_MAPPERS)
    @Operation(summary = "Get mappers by name for a specific protocol")
    public Stream<ProtocolMapperRepresentation> getMappersPerProtocol(@PathParam("protocol") String protocol) {
        viewPermission.require();

        return client.getProtocolMappersStream()
                .filter(mapper -> isEnabled(session, mapper) && Objects.equals(mapper.getProtocol(), protocol))
                .map(this::toEffectiveProtocolMapperRep);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.PROTOCOL_MAPPERS)
    @Operation(summary = "Create a mapper")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Created"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response createMapper(ProtocolMapperRepresentation rep) {
        managePermission.require();

        ProtocolMapperModel model = null;
        try {
            model = RepresentationToModel.toModel(rep);
            validateModel(model);
            model = client.addProtocolMapper(model);
            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), model.getId()).representation(rep).success();

        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Protocol mapper exists with same name");
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.PROTOCOL_MAPPERS)
    @Operation(summary = "Create multiple mappers")
    @APIResponse(responseCode = "204", description = "No Content")
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.PROTOCOL_MAPPERS)
    @Operation(summary = "Get mappers")
    public Stream<ProtocolMapperRepresentation> getMappers() {
        viewPermission.require();

        return client.getProtocolMappersStream()
                .filter(mapper -> isEnabled(session, mapper))
                .map(this::toEffectiveProtocolMapperRep);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.PROTOCOL_MAPPERS)
    @Operation(summary = "Get mapper by id")
    public ProtocolMapperRepresentation getMapperById(@Parameter(description = "Mapper id") @PathParam("id") String id) {
        viewPermission.require();

        ProtocolMapperModel model = client.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        return toEffectiveProtocolMapperRep(model);
    }

    private ProtocolMapperRepresentation toEffectiveProtocolMapperRep(ProtocolMapperModel model) {
        ProtocolMapper mapper = (ProtocolMapper) session.getKeycloakSessionFactory().getProviderFactory(ProtocolMapper.class, model.getProtocolMapper());
        if (mapper == null) {
            logger.warnf("Protocol mapper provider '%s' not found. Configured on mapper with ID '%s'", model.getProtocolMapper(), model.getId());
            throw new NotFoundException("Protocol mapper provider not found");
        }

        model = mapper.getEffectiveModel(session, realm, model);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.PROTOCOL_MAPPERS)
    @Operation(summary = "Update the mapper")
    public void update(@Parameter(description = "Mapper id") @PathParam("id") String id, ProtocolMapperRepresentation rep) {
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.PROTOCOL_MAPPERS)
    @Operation(summary = "Delete the mapper")
    public void delete(@Parameter(description = "Mapper id") @PathParam("id") String id) {
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
