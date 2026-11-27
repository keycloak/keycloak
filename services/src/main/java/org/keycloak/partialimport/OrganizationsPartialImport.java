/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.partialimport;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.validation.OrganizationsValidation.OrganizationValidationException;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.utils.StringUtil;

public class OrganizationsPartialImport extends AbstractPartialImport<OrganizationRepresentation> {

    @Override
    public List<OrganizationRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getOrganizations();
    }

    @Override
    public String getName(OrganizationRepresentation orgRep) {
        return StringUtil.isNullOrEmpty(orgRep.getAlias()) ? orgRep.getName() : orgRep.getAlias();
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, OrganizationRepresentation orgRep) {
        return findOrganization(session, orgRep).getId();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, OrganizationRepresentation orgRep) {
        return findOrganization(session, orgRep) != null;
    }

    @Override
    public String existsMessage(RealmModel realm, OrganizationRepresentation orgRep) {
        return "Organization '" + getName(orgRep) + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.ORGANIZATION;
    }

    @Override
    public void prepare(PartialImportRepresentation partialImportRep, RealmModel realm, KeycloakSession session) {
        super.prepare(partialImportRep, realm, session);
        if (!toOverwrite.isEmpty()) {
            throw overwriteRefused(realm, toOverwrite.iterator().next());
        }
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, OrganizationRepresentation orgRep) {
        throw overwriteRefused(realm, orgRep);
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, OrganizationRepresentation orgRep) {
        for (IdentityProviderRepresentation idpRep : Optional.ofNullable(orgRep.getIdentityProviders()).orElse(List.of())) {
            if (session.identityProviders().getByAlias(idpRep.getAlias()) == null) {
                throw ErrorResponse.error("Identity provider '" + idpRep.getAlias() + "' linked to organization '" + getName(orgRep) + "' does not exist.", Response.Status.BAD_REQUEST);
            }
        }
        orgRep.setId(null);
        clearIds(orgRep.getGroups());
        try {
            RepresentationToModel.importOrganization(session, realm, orgRep);
        } catch (OrganizationValidationException e) {
            throw ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            throw ErrorResponse.error("Invalid configuration for organization '" + getName(orgRep) + "': " + e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    private OrganizationModel findOrganization(KeycloakSession session, OrganizationRepresentation orgRep) {
        return session.getProvider(OrganizationProvider.class).getByAlias(getName(orgRep));
    }

    private ErrorResponseException overwriteRefused(RealmModel realm, OrganizationRepresentation orgRep) {
        return ErrorResponse.exists(existsMessage(realm, orgRep) + " The partial import does not overwrite organizations.");
    }

    private static void clearIds(List<GroupRepresentation> groups) {
        for (GroupRepresentation group : Optional.ofNullable(groups).orElse(List.of())) {
            group.setId(null);
            clearIds(group.getSubGroups());
        }
    }
}
