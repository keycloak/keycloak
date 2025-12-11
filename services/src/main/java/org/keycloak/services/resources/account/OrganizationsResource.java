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
package org.keycloak.services.resources.account;

import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.AccountRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.account.OrganizationRepresentation;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.Auth;

public class OrganizationsResource {

    private final KeycloakSession session;
    private final UserModel user;
    private final Auth auth;

    public OrganizationsResource(KeycloakSession session,
                                 Auth auth,
                                 UserModel user) {
        this.session = session;
        this.auth = auth;
        this.user = user;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrganizations() {
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);
        return Cors.builder().auth()
                .allowedOrigins(auth.getToken())
                .add(Response.ok(session.getProvider(OrganizationProvider.class)
                        .getByMember(user)
                        .map(this::toRepresentation))
                );
    }

    private OrganizationRepresentation toRepresentation(OrganizationModel model) {
        OrganizationRepresentation rep = new OrganizationRepresentation();

        rep.setId(model.getId());
        rep.setName(model.getName());
        rep.setAlias(model.getAlias());
        rep.setDescription(model.getDescription());
        rep.setEnabled(model.isEnabled());
        rep.setDomains(model.getDomains().map(OrganizationDomainModel::getName).collect(Collectors.toSet()));

        return rep;
    }
}
