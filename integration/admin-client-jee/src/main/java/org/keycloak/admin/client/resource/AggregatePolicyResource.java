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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface AggregatePolicyResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    AggregatePolicyRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(AggregatePolicyRepresentation representation);

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
