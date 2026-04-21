/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

public interface OrganizationIdentityProviderResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    IdentityProviderRepresentation toRepresentation();

    @DELETE
    Response delete();

    /**
     * Returns organization groups for the identity provider with the specified alias.
     * It allows filtering and displaying only the organization groups that are valid for the given identity provider.
     *
     * Only returns groups if the identity provider is associated with the organization and the organization
     * is enabled. Otherwise, returns an error or empty stream.
     *
     * @param search a string to search for in group names
     * @param searchQuery a query to search for group attributes, in the format 'key1:value1 key2:value2'
     * @param exact if true, perform exact match on the search parameter
     * @param first the position of the first result (pagination offset)
     * @param max the maximum number of results to return
     * @param briefRepresentation if true, return brief group representation; otherwise return full representation
     * @return a stream of organization groups associated with the organization
     * @since Keycloak server 26.6.0
     */
    @Path("groups")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<GroupRepresentation> getGroups(@QueryParam("search") String search,
                                        @QueryParam("q") String searchQuery,
                                        @QueryParam("exact") @DefaultValue("false") Boolean exact,
                                        @QueryParam("first") Integer first,
                                        @QueryParam("max") Integer max,
                                        @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation,
                                        @QueryParam("subGroupsCount") @DefaultValue("false") boolean subGroupsCount);
}
