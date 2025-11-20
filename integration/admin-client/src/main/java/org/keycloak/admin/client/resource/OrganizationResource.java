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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.OrganizationRepresentation;

public interface OrganizationResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    OrganizationRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response update(OrganizationRepresentation organization);

    @DELETE
    Response delete();

    @Path("members")
    OrganizationMembersResource members();

    @Path("identity-providers")
    OrganizationIdentityProvidersResource identityProviders();
}
