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
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import java.util.Objects;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UserResource;
import org.keycloak.services.resources.admin.UsersResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.utils.StringUtil;

@Provider
public class OrganizationMemberResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationProvider provider;
    private final OrganizationModel organization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public OrganizationMemberResource() {
        this.session = null;
        this.realm = null;
        this.provider = null;
        this.organization = null;
        this.auth = null;
        this.adminEvent = null;
    }

    public OrganizationMemberResource(KeycloakSession session, OrganizationModel organization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.provider = session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMember(String id) {
        auth.realm().requireManageRealm();
        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            throw ErrorResponse.error("User does not exist", Status.BAD_REQUEST);
        }

        try {
            if (provider.addMember(organization, user)) {
                return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(user.getId()).build()).build();
            }
        } catch (ModelException me) {
            throw ErrorResponse.error(me.getMessage(), Status.BAD_REQUEST);
        }

        throw ErrorResponse.error("User is already a member of the organization.", Status.CONFLICT);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserRepresentation> getMembers() {
        auth.realm().requireManageRealm();
        return provider.getMembersStream(organization).map(this::toRepresentation);
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation get(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        if (StringUtil.isBlank(id)) {
            throw ErrorResponse.error("id cannot be null", Status.BAD_REQUEST);
        }

        return toRepresentation(getMember(id));
    }

    @Path("{id}")
    @DELETE
    public Response delete(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        if (StringUtil.isBlank(id)) {
            throw ErrorResponse.error("id cannot be null", Status.BAD_REQUEST);
        }

        UserModel member = getMember(id);

        if (provider.removeMember(organization, member)) {
            return Response.noContent().build();
        }

        throw ErrorResponse.error("Not a member of the organization", Status.BAD_REQUEST);
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") String id, UserRepresentation user) {
        auth.realm().requireManageRealm();
        return new UserResource(session, getMember(id), auth, adminEvent).updateUser(user);
    }

    @Path("{id}/organization")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OrganizationRepresentation getOrganization(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        if (StringUtil.isBlank(id)) {
            throw ErrorResponse.error("id cannot be null", Status.BAD_REQUEST);
        }

        UserModel member = getMember(id);
        OrganizationModel organization = provider.getByMember(member);

        if (organization == null) {
            throw ErrorResponse.error("Not associated with an organization", Status.NOT_FOUND);
        }

        OrganizationRepresentation rep = new OrganizationRepresentation();

        rep.setId(organization.getId());

        return rep;
    }

    private UserModel getMember(String id) {
        UserModel member = provider.getMemberById(organization, id);

        if (member == null) {
            throw new NotFoundException();
        }

        return member;
    }

    private UserRepresentation toRepresentation(UserModel member) {
        return ModelToRepresentation.toRepresentation(session, realm, member);
    }
}
