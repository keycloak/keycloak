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
package org.keycloak.services.resources.account.resources;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.Auth;
import org.keycloak.utils.MediaType;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcesService extends AbstractResourceService {

    public ResourcesService(KeycloakSession session, UserModel user, Auth auth, HttpRequest request) {
        super(session, user, auth, request);
    }

    /**
     * Returns a list of {@link Resource} where the {@link #user} is the resource owner.
     *
     * @return a list of {@link Resource} where the {@link #user} is the resource owner
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResources() {
        return cors(Response.ok(resourceStore.findByOwner(user.getId(), null).stream()
                .map(resource -> new Resource(resource, user, provider))
                .collect(Collectors.toList())));
    }

    /**
     * Returns a list of {@link Resource} shared with the {@link #user}
     *
     * @return a list of {@link Resource} shared with the {@link #user}
     */
    @GET
    @Path("shared-with-me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSharedWithMe() {
        return cors(Response.ok(getPermissions(ticketStore.findGranted(user.getId(), null), false)));
    }

    /**
     * Returns a list of {@link Resource} where the {@link #user} is the resource owner and the resource is 
     * shared with other users.
     *
     * @return a list of {@link Resource} where the {@link #user} is the resource owner and the resource is 
     *      * shared with other users
     */
    @GET
    @Path("shared-with-others")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSharedWithOthers() {
        Map<String, String> filters = new HashMap<>();

        filters.put(PermissionTicket.OWNER, user.getId());
        filters.put(PermissionTicket.GRANTED, Boolean.TRUE.toString());

        return cors(Response.ok(getPermissions(ticketStore.find(filters, null, -1, -1), true)));
    }

    @Path("{id}")
    public Object getResource(@PathParam("id") String id) {
        org.keycloak.authorization.model.Resource resource = resourceStore.findById(id, null);

        if (resource == null) {
            throw new NotFoundException("resource_not_found");
        }

        if (!resource.getOwner().equals(user.getId())) {
            throw new BadRequestException("invalid_resource");
        }
        
        return new ResourceService(resource, provider.getKeycloakSession(), user, auth, request);
    }
}
