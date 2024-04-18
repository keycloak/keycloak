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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
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
public class OrganizationIdentityProviderResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationProvider organizationProvider;
    private final OrganizationModel organization;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;

    public OrganizationIdentityProviderResource() {
        // needed for registering to the JAX-RS stack
        this(null, null, null, null);
    }

    public OrganizationIdentityProviderResource(KeycloakSession session, OrganizationModel organization, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
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

        IdentityProviderModel identityProvider = organization.getIdentityProvider();
        if (identityProvider != null) {
            throw ErrorResponse.error("Organization already assigned with an identity provider.", Status.BAD_REQUEST);
        }

        //create IdP within the realm
        Response response = new IdentityProvidersResource(realm, session, auth, adminEvent).create(providerRep);

        if (Status.CREATED.getStatusCode() == response.getStatus()) {

            //get the created IdP from session
            identityProvider = realm.getIdentityProviderByAlias(providerRep.getAlias());

            String errorMessage;
            try {
                if (organizationProvider.addIdentityProvider(organization, identityProvider)) {
                    return response;
                }
                errorMessage = "Assigning the Identity provider with the organization was not succesful.";
            } catch (ModelException me) {
                errorMessage = me.getMessage();
            }
            throw ErrorResponse.error(errorMessage, Status.BAD_REQUEST);
        }

        return response;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityProviderRepresentation getIdentityProvider() {
        return Optional.ofNullable(organization.getIdentityProvider()).map(this::toRepresentation).orElse(null);
    }

    @DELETE
    public Response delete() {
        IdentityProviderModel identityProvider = getIdentityProviderModel();

        Response response = getIdentityProviderResource(identityProvider).delete();

        // remove link between IdP and the organization if the IdP deletetion was successful
        if (Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
            String errorMessage;
            try {
                if (organizationProvider.removeIdentityProvider(organization)) {
                    return response;
                }
                errorMessage = "Removing the Identity provider from the organization was not succesful.";
            } catch (ModelException me) {
                errorMessage = me.getMessage();
            }
            throw ErrorResponse.error(errorMessage, Status.BAD_REQUEST);
        }

        return response;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(IdentityProviderRepresentation rep) {
        IdentityProviderModel identityProvider = getIdentityProviderModel();

        if (!rep.getAlias().equals(identityProvider.getAlias()) || (rep.getInternalId() != null && !Objects.equals(rep.getInternalId(), identityProvider.getInternalId()))) {
            throw ErrorResponse.error("Identity provider not assigned to the organization.", Status.NOT_FOUND);
        }

        Response response = getIdentityProviderResource(identityProvider).update(rep);

        //update link between IdP and the organization if the update of IdP was successful and the IdP alias differs
        if (Status.NO_CONTENT.getStatusCode() == response.getStatus() && 
                ! Objects.equals(identityProvider.getAlias(), rep.getAlias())) {

            //get the updated IdP from session
            identityProvider = realm.getIdentityProviderByAlias(rep.getAlias());

            String errorMessage;
            try {
                if (organizationProvider.removeIdentityProvider(organization) && 
                    organizationProvider.addIdentityProvider(organization, identityProvider)) {
                    return response;
                }
                errorMessage = "Updating the Identity provider was not succesful.";
            } catch (ModelException me) {
                errorMessage = me.getMessage();
            }
            throw ErrorResponse.error(errorMessage, Status.BAD_REQUEST);
        }

        return response;
    }

    private IdentityProviderRepresentation toRepresentation(IdentityProviderModel idp) {
        return ModelToRepresentation.toRepresentation(realm, idp);
    }

    private IdentityProviderResource getIdentityProviderResource(IdentityProviderModel idp) {
        return new IdentityProviderResource(auth, realm, session, idp, adminEvent);
    }

    private IdentityProviderModel getIdentityProviderModel() {
        IdentityProviderModel identityProvider = organization.getIdentityProvider();

        if (identityProvider == null) {
            throw ErrorResponse.error("Organization doesn't have assigned an identity provider.", Status.NOT_FOUND);
        }

        return identityProvider;
    }
}
