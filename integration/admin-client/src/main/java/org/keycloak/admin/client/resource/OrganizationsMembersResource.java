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

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.idm.OrganizationRepresentation;

public interface OrganizationsMembersResource {

    /**
     * Returns the organizations associated with the user that has the specified id.
     *
     * @param id user id
     * @return Organizations of the user
     */
    @Path("{id}/organizations")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> getOrganizations(
            @PathParam("id") String id);

    /**
     * Returns the organizations associated with the user that has the specified id.
     *
     * @param id user id
     * @param briefRepresentation if false, return the full representation. Otherwise, only the basic fields are returned. It is true by default. Parameter supported since Keycloak 26.3. It is assumed to be false for the older Keycloak server versions.
     * @return Organizations of the user
     */
    @Path("{id}/organizations")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> getOrganizations(
            @PathParam("id") String id,
            @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);
}
