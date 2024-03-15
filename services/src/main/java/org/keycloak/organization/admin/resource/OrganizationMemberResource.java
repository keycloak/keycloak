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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.UsersResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

@Provider
public class OrganizationMemberResource {

    private final KeycloakSession session;
    private final OrganizationProvider provider;
    private final OrganizationModel organization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public OrganizationMemberResource() {
        this.session = null;
        this.provider = null;
        this.organization = null;
        this.auth = null;
        this.adminEvent = null;
    }

    public OrganizationMemberResource(KeycloakSession session, OrganizationModel organization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.provider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMember(UserRepresentation rep) {
        UsersResource usersResource = new UsersResource(session, auth, adminEvent);
        Response response = usersResource.createUser(rep);

        if (Status.CREATED.getStatusCode() == response.getStatus()) {
            RealmModel realm = session.getContext().getRealm();
            UserModel member = session.users().getUserByUsername(realm, rep.getEmail());

            if (organization.addMember(member)) {
                return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(member.getId()).build()).build();
            }
        }

        throw new BadRequestException();
    }
}
