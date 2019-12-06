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
import org.keycloak.common.ClientConnection;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base resource for managing users
 *
 * @resource Role Mapper
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:mpaulosnunes@gmail.com">Miguel P. Nunes</a>
 * @version $Revision: 1 $
 */
public class RoleMapperResource {

    protected static final Logger logger = Logger.getLogger(RoleMapperResource.class);

    protected RealmModel realm;

    private RoleMapperModel roleMapper;

    private AdminEventBuilder adminEvent;

    protected AdminPermissionEvaluator.RequirePermissionCheck managePermission;
    protected AdminPermissionEvaluator.RequirePermissionCheck viewPermission;
    private AdminPermissionEvaluator auth;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

    public RoleMapperResource(RealmModel realm,
                              AdminPermissionEvaluator auth,
                              RoleMapperModel roleMapper,
                              AdminEventBuilder adminEvent,
                              AdminPermissionEvaluator.RequirePermissionCheck manageCheck,
                              AdminPermissionEvaluator.RequirePermissionCheck viewCheck) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.REALM_ROLE_MAPPING);
        this.roleMapper = roleMapper;
        this.managePermission = manageCheck;
        this.viewPermission = viewCheck;

    }

    /**
     * Get role mappings
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public MappingsRepresentation getRoleMappings() {
        viewPermission.require();

        List<RoleRepresentation> realmRolesRepresentation = new ArrayList<>();
        Map<String, ClientMappingsRepresentation> appMappings = new HashMap<>();

        ClientModel clientModel;
        ClientMappingsRepresentation mappings;

        for (RoleModel roleMapping : roleMapper.getRoleMappings()) {
            RoleContainerModel container = roleMapping.getContainer();
            if (container instanceof RealmModel) {
                realmRolesRepresentation.add(ModelToRepresentation.toBriefRepresentation(roleMapping));
            } else if (container instanceof ClientModel) {
                clientModel = (ClientModel) container;
                if ((mappings = appMappings.get(clientModel.getClientId())) == null) {
                    mappings = new ClientMappingsRepresentation();
                    mappings.setId(clientModel.getId());
                    mappings.setClient(clientModel.getClientId());
                    mappings.setMappings(new ArrayList<>());
                    appMappings.put(clientModel.getClientId(), mappings);
                }
                mappings.getMappings().add(ModelToRepresentation.toBriefRepresentation(roleMapping));
            }
        }

        MappingsRepresentation all = new MappingsRepresentation();
        if (!realmRolesRepresentation.isEmpty()) all.setRealmMappings(realmRolesRepresentation);
        if (!appMappings.isEmpty()) all.setClientMappings(appMappings);

        return all;
    }

    /**
     * Get realm-level role mappings
     *
     * @return
     */
    @Path("realm")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getRealmRoleMappings() {
        viewPermission.require();

        Set<RoleModel> realmMappings = roleMapper.getRealmRoleMappings();
        List<RoleRepresentation> realmMappingsRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : realmMappings) {
            realmMappingsRep.add(ModelToRepresentation.toBriefRepresentation(roleModel));
        }
        return realmMappingsRep;
    }

    /**
     * Get effective realm-level role mappings
     *
     * This will recurse all composite roles to get the result.
     *
     * @return
     */
    @Path("realm/composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getCompositeRealmRoleMappings() {
        viewPermission.require();

        Set<RoleModel> roles = realm.getRoles();
        List<RoleRepresentation> realmMappingsRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (roleMapper.hasRole(roleModel)) {
               realmMappingsRep.add(ModelToRepresentation.toBriefRepresentation(roleModel));
            }
        }
        return realmMappingsRep;
    }

    /**
     * Get realm-level roles that can be mapped
     *
     * @return
     */
    @Path("realm/available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getAvailableRealmRoleMappings() {
        viewPermission.require();

        Set<RoleModel> available = realm.getRoles();
        Set<RoleModel> set = available.stream().filter(r ->
            canMapRole(r)
        ).collect(Collectors.toSet());
        return ClientRoleMappingsResource.getAvailableRoles(roleMapper, set);
    }

    /**
     * Add realm-level role mappings to the user
     *
     * @param roles Roles to add
     */
    @Path("realm")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addRealmRoleMappings(List<RoleRepresentation> roles) {
        managePermission.require();

        logger.debugv("** addRealmRoleMappings: {0}", roles);

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRole(role.getName());
            if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                throw new NotFoundException("Role not found");
            }
            auth.roles().requireMapRole(roleModel);
            roleMapper.grantRole(roleModel);
        }

        adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).representation(roles).success();
    }

    /**
     * Delete realm-level role mappings
     *
     * @param roles
     */
    @Path("realm")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteRealmRoleMappings(List<RoleRepresentation> roles) {
        managePermission.require();

        logger.debug("deleteRealmRoleMappings");
        if (roles == null) {
            Set<RoleModel> roleModels = roleMapper.getRealmRoleMappings();
            roles = new LinkedList<>();

            for (RoleModel roleModel : roleModels) {
                auth.roles().requireMapRole(roleModel);
                roleMapper.deleteRoleMapping(roleModel);
                roles.add(ModelToRepresentation.toBriefRepresentation(roleModel));
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                auth.roles().requireMapRole(roleModel);
                try {
                    roleMapper.deleteRoleMapping(roleModel);
                } catch (ModelException me) {
                    Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
                    throw new ErrorResponseException(me.getMessage(), MessageFormat.format(messages.getProperty(me.getMessage(), me.getMessage()), me.getParameters()),
                            Response.Status.BAD_REQUEST);
                }
            }

        }

        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(roles).success();

    }

    private boolean canMapRole(RoleModel roleModel) {
        return auth.roles().canMapRole(roleModel);
    }

    @Path("clients/{client}")
    public ClientRoleMappingsResource getUserClientRoleMappingsResource(@PathParam("client") String client) {
        ClientModel clientModel = realm.getClientById(client);
        if (clientModel == null) {
            throw new NotFoundException("Client not found");
        }
        ClientRoleMappingsResource resource = new ClientRoleMappingsResource(session.getContext().getUri(), session, realm, auth, roleMapper,
                clientModel, adminEvent,
                managePermission, viewPermission);
        return resource;

    }
}
