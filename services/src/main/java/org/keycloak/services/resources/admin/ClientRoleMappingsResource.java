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
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientRoleMappingsResource {
    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    protected KeycloakSession session;
    protected RealmModel realm;
    protected RealmAuth auth;
    protected RoleMapperModel user;
    protected ClientModel client;
    protected AdminEventBuilder adminEvent;
    private UriInfo uriInfo;

    public ClientRoleMappingsResource(UriInfo uriInfo, KeycloakSession session, RealmModel realm, RealmAuth auth, RoleMapperModel user, ClientModel client, AdminEventBuilder adminEvent) {
        this.uriInfo = uriInfo;
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.user = user;
        this.client = client;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_ROLE_MAPPING);
    }

    /**
     * Get client-level role mappings for the user, and the app
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getClientRoleMappings() {
        auth.requireView();

        if (user == null || client == null) {
            throw new NotFoundException("Not found");
        }

        Set<RoleModel> mappings = user.getClientRoleMappings(client);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mapRep;
    }

    /**
     * Get effective client-level role mappings
     *
     * This recurses any composite roles
     *
     * @return
     */
    @Path("composite")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getCompositeClientRoleMappings() {
        auth.requireView();

        if (user == null || client == null) {
            throw new NotFoundException("Not found");
        }

        Set<RoleModel> roles = client.getRoles();
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            if (user.hasRole(roleModel)) mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mapRep;
    }

    /**
     * Get available client-level roles that can be mapped to the user
     *
     * @return
     */
    @Path("available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<RoleRepresentation> getAvailableClientRoleMappings() {
        auth.requireView();

        if (user == null || client == null) {
            throw new NotFoundException("Not found");
        }

        Set<RoleModel> available = client.getRoles();
        return getAvailableRoles(user, available);
    }

    public static List<RoleRepresentation> getAvailableRoles(RoleMapperModel mapper, Set<RoleModel> available) {
        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (RoleModel roleModel : available) {
            if (mapper.hasRole(roleModel)) continue;
            roles.add(roleModel);
        }

        List<RoleRepresentation> mappings = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : roles) {
            mappings.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        return mappings;
    }

    /**
     * Add client-level roles to the user role mapping
     *
     * @param roles
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void addClientRoleMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        if (user == null || client == null) {
            throw new NotFoundException("Not found");
        }

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = client.getRole(role.getName());
            if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                throw new NotFoundException("Role not found");
            }
            user.grantRole(roleModel);
        }
        adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo).representation(roles).success();

    }

    /**
     * Delete client-level roles from user role mapping
     *
     * @param roles
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteClientRoleMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        if (user == null || client == null) {
            throw new NotFoundException("Not found");
        }

        if (roles == null) {
            Set<RoleModel> roleModels = user.getClientRoleMappings(client);
            roles = new LinkedList<>();

            for (RoleModel roleModel : roleModels) {
                if (roleModel.getContainer() instanceof ClientModel) {
                    ClientModel client = (ClientModel) roleModel.getContainer();
                    if (!client.getId().equals(this.client.getId())) continue;
                }
                user.deleteRoleMapping(roleModel);
                roles.add(ModelToRepresentation.toRepresentation(roleModel));
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = client.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }

                try {
                    user.deleteRoleMapping(roleModel);
                } catch (ModelException me) {
                    Properties messages = AdminRoot.getMessages(session, realm, auth.getAuth().getToken().getLocale());
                    throw new ErrorResponseException(me.getMessage(), MessageFormat.format(messages.getProperty(me.getMessage(), me.getMessage()), me.getParameters()),
                            Response.Status.BAD_REQUEST);
                }
            }
        }

        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).representation(roles).success();
    }
}
