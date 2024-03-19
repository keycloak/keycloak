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

package org.keycloak.organization.admin.resource;

import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.utils.StringUtil;

@Provider
public class OrganizationResource {

    private final KeycloakSession session;
    private final OrganizationProvider provider;

    public OrganizationResource() {
        // needed for registering to the JAX-RS stack
        this(null);
    }

    public OrganizationResource(KeycloakSession session) {
        this.session = session;
        this.provider = session == null ? null : session.getProvider(OrganizationProvider.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(OrganizationRepresentation organization) {
        if (organization == null) {
            throw new BadRequestException();
        }

        RealmModel realm = session.getContext().getRealm();
        OrganizationModel model = provider.createOrganization(realm, organization.getName());

        return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<OrganizationRepresentation> get() {
        return provider.getOrganizationsStream(session.getContext().getRealm()).map(this::toRepresentation);
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OrganizationRepresentation get(@PathParam("id") String id) {
        if (StringUtil.isBlank(id)) {
            throw new BadRequestException();
        }

        return toRepresentation(getOrganization(session.getContext().getRealm(), id));
    }

    @Path("{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        if (StringUtil.isBlank(id)) {
            throw new BadRequestException();
        }

        RealmModel realm = session.getContext().getRealm();
        provider.removeOrganization(realm, getOrganization(realm, id));

        return Response.noContent().build();
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") String id, OrganizationRepresentation organization) {
        RealmModel realm = session.getContext().getRealm();
        OrganizationModel model = getOrganization(realm, id);

        toModel(organization, model);

        return Response.noContent().build();
    }

    private OrganizationModel getOrganization(RealmModel realm, String id) {
        if (id == null) {
            throw new BadRequestException();
        }

        OrganizationModel model = provider.getOrganizationById(realm, id);

        if (model == null) {
            throw new NotFoundException();
        }

        return model;
    }

    private OrganizationRepresentation toRepresentation(OrganizationModel model) {
        if (model == null) {
            return null;
        }

        OrganizationRepresentation rep = new OrganizationRepresentation();

        rep.setId(model.getId());
        rep.setName(model.getName());

        return rep;
    }

    private OrganizationModel toModel(OrganizationRepresentation rep, OrganizationModel model) {
        if (rep == null) {
            return null;
        }

        model.setName(rep.getName());

        return model;
    }
}
