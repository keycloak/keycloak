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

package org.keycloak.admin.client.resource;

import org.jboss.resteasy.annotations.cache.NoCache;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface GroupResource {

    /**
     * Does not expand hierarchy.  Subgroups will not be set.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation toRepresentation();

    /**
     * Update group
     *
     * @param rep
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(GroupRepresentation rep);

    @DELETE
    public void remove();


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
    public Response subGroup(GroupRepresentation rep);


    @Path("role-mappings")
    public RoleMappingResource roles();

    /**
     * Get users
     * <p/>
     * Returns a list of users, filtered according to query parameters
     *
     * @param firstResult Pagination offset
     * @param maxResults  Pagination size
     * @return
     */
    @GET
    @NoCache
    @Path("/members")
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserRepresentation> members(@QueryParam("first") Integer firstResult,
                                            @QueryParam("max") Integer maxResults);
}
