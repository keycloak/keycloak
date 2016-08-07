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
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Bill Burke
 */
public class GroupResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private final RealmAuth auth;
    private final AdminEventBuilder adminEvent;
    private final GroupModel group;

    public GroupResource(RealmModel realm, GroupModel group, KeycloakSession session, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.GROUP);
        this.group = group;
    }

    @Context private UriInfo uriInfo;

    /**
     *
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation getGroup() {
        this.auth.requireView();

        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        return ModelToRepresentation.toGroupHierarchy(group, true);
    }

    /**
     * Update group, ignores subgroups.
     *
     * @param rep
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateGroup(GroupRepresentation rep) {
        this.auth.requireManage();

        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        updateGroup(rep, group);
        adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(rep).success();


    }

    @DELETE
    public void deleteGroup() {
        this.auth.requireManage();

        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        realm.removeGroup(group);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }


    /**
     * Set or create child.  This will just set the parent if it exists.  Create it and set the parent
     * if the group doesn't exist.
     *
     * @param rep
     */
    @POST
    @Path("children")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addChild(GroupRepresentation rep) {
        this.auth.requireManage();

        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        Response.ResponseBuilder builder = Response.status(204);
        GroupModel child = null;
        if (rep.getId() != null) {
            child = realm.getGroupById(rep.getId());
            if (child == null) {
                throw new NotFoundException("Could not find child by id");
            }
            adminEvent.operation(OperationType.UPDATE);
        } else {
            child = realm.createGroup(rep.getName());
            updateGroup(rep, child);
            URI uri = uriInfo.getBaseUriBuilder()
                                           .path(uriInfo.getMatchedURIs().get(2))
                                           .path(child.getId()).build();
            builder.status(201).location(uri);
            rep.setId(child.getId());
            adminEvent.operation(OperationType.CREATE);

        }
        realm.moveGroup(child, group);
        adminEvent.resourcePath(uriInfo).representation(rep).success();

        GroupRepresentation childRep = ModelToRepresentation.toGroupHierarchy(child, true);
        return builder.type(MediaType.APPLICATION_JSON_TYPE).entity(childRep).build();
    }

    public static void updateGroup(GroupRepresentation rep, GroupModel model) {
        if (rep.getName() != null) model.setName(rep.getName());

        if (rep.getAttributes() != null) {
            Set<String> attrsToRemove = new HashSet<>(model.getAttributes().keySet());
            attrsToRemove.removeAll(rep.getAttributes().keySet());
            for (Map.Entry<String, List<String>> attr : rep.getAttributes().entrySet()) {
                model.setAttribute(attr.getKey(), attr.getValue());
            }

            for (String attr : attrsToRemove) {
                model.removeAttribute(attr);
            }
        }
    }

    @Path("role-mappings")
    public RoleMapperResource getRoleMappings() {
        auth.init(RealmAuth.Resource.USER);

        RoleMapperResource resource =  new RoleMapperResource(realm, auth, group, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;

    }

    /**
     * Get users
     *
     * Returns a list of users, filtered according to query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults Pagination size
     * @return
     */
    @GET
    @NoCache
    @Path("members")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserRepresentation> getMembers(@QueryParam("first") Integer firstResult,
                                               @QueryParam("max") Integer maxResults) {
        auth.requireView();

        if (group == null) {
            throw new NotFoundException("Could not find group by id");
        }

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : -1;

        List<UserRepresentation> results = new ArrayList<UserRepresentation>();
        List<UserModel> userModels = session.users().getGroupMembers(realm, group, firstResult, maxResults);

        for (UserModel user : userModels) {
            results.add(ModelToRepresentation.toRepresentation(user));
        }
        return results;
    }

}
