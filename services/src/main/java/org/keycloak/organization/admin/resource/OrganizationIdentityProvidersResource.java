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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.IdentityProviderResource;
import org.keycloak.services.resources.admin.IdentityProvidersResource;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

@Provider
public class OrganizationIdentityProvidersResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationProvider organizationProvider;
    private final OrganizationModel organization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public OrganizationIdentityProvidersResource() {
        // needed for registering to the JAX-RS stack
        this(null, null, null, null);
    }

    public OrganizationIdentityProvidersResource(KeycloakSession session, OrganizationModel organization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = session == null ? null : session.getContext().getRealm();
        this.organizationProvider = session == null ? null : session.getProvider(OrganizationProvider.class);
        this.organization = organization;
        this.auth = auth;
        this.adminEvent = adminEvent;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addIdentityProvider(IdentityProviderRepresentation providerRep) {
        auth.realm().requireManageRealm();

        Response response = new IdentityProvidersResource(realm, session, auth, adminEvent).create(providerRep);

        if (Status.CREATED.getStatusCode() == response.getStatus()) {
            try {
                IdentityProviderModel identityProvider = realm.getIdentityProviderByAlias(providerRep.getAlias());

                if (organizationProvider.addIdentityProvider(organization, identityProvider)) {
                    return response;
                }

                throw ErrorResponse.error("Identity provider already associated to the organization", Status.BAD_REQUEST);
            } catch (ModelException me) {
                throw ErrorResponse.error(me.getMessage(), Status.BAD_REQUEST);
            }
        }

        return response;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<IdentityProviderRepresentation> getIdentityProviders() {
        auth.realm().requireManageRealm();
        return organization.getIdentityProviders().map(this::toRepresentation);
    }

    @Path("{alias}")
    public IdentityProviderResource getIdentityProvider(@PathParam("alias") String alias) {
        IdentityProviderModel broker = realm.getIdentityProviderByAlias(alias);
        return new IdentityProviderResource(auth, realm, session, broker, adminEvent) {
            @Override
            public Response delete() {
                Response response = super.delete();

                if (organizationProvider.removeIdentityProvider(organization, broker)) {
                    return response;
                }

                throw ErrorResponse.error("Identity provider not associated with the organization", Status.BAD_REQUEST);
            }

            @Override
            public Response update(IdentityProviderRepresentation providerRep) {
                if (organization.getIdentityProviders().noneMatch(model -> model.getInternalId().equals(providerRep.getInternalId()) || model.getAlias().equals(providerRep.getAlias()))) {
                    return Response.status(Status.NOT_FOUND).build();
                }
                String domain = providerRep.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);

                if (domain != null && organization.getDomains().map(OrganizationDomainModel::getName).noneMatch(domain::equals)) {
                    return Response.status(Status.BAD_REQUEST).build();
                }

                return super.update(providerRep);
            }
        };
    }

    private IdentityProviderRepresentation toRepresentation(IdentityProviderModel idp) {
        return ModelToRepresentation.toRepresentation(realm, idp);
    }
}
