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

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface RolePolicyResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    RolePolicyRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(RolePolicyRepresentation representation);

    @DELETE
    void remove();

    @Path("/associatedPolicies")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<PolicyRepresentation> associatedPolicies();

    @Path("/dependentPolicies")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<PolicyRepresentation> dependentPolicies();

    @Path("/resources")
    @GET
    @Produces("application/json")
    List<ResourceRepresentation> resources();

}
