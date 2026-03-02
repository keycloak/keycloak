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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationMembershipRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;

/**
 * PartialImport handler for organization memberships.
 * This handler manages importing user-to-organization memberships.
 *
 * @author Christian Jeschke
 */
public class OrganizationMembershipsPartialImport extends AbstractPartialImport<OrganizationMembershipRepresentation> {

    // Cache for organization names during import
    private final Map<String, String> organizationNames = new HashMap<>();

    // Cache for created membership IDs
    private final Map<String, String> createdIds = new HashMap<>();

    @Override
    public List<OrganizationMembershipRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getOrganizationMemberships();
    }

    @Override
    public String getName(OrganizationMembershipRepresentation membership) {
        // Try to get cached organization name, fallback to ID
        String orgName = organizationNames.getOrDefault(membership.getOrganizationId(), membership.getOrganizationId());
        return membership.getUsername() + " -> " + orgName;
    }

    private String makeKey(OrganizationMembershipRepresentation membership) {
        return membership.getOrganizationId() + ":" + membership.getUsername();
    }

    @Override
    public void prepare(PartialImportRepresentation partialImportRep, RealmModel realm, KeycloakSession session) {
        // Pre-load organization names for better display
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        if (provider == null) {
            super.prepare(partialImportRep, realm, session);
            return;
        }

        List<OrganizationMembershipRepresentation> memberships = getRepList(partialImportRep);
        if (memberships != null) {
            for (OrganizationMembershipRepresentation membership : memberships) {
                var organization = provider.getById(membership.getOrganizationId());
                if (organization != null) {
                    organizationNames.put(membership.getOrganizationId(), organization.getName());
                }
            }
        }
        super.prepare(partialImportRep, realm, session);
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, OrganizationMembershipRepresentation membership) {
        String key = makeKey(membership);

        String cachedId = createdIds.get(key);
        if (cachedId != null) {
            return cachedId;
        }

        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        if (provider == null) {
            return null;
        }

        var organization = provider.getById(membership.getOrganizationId());
        if (organization == null) {
            return null;
        }

        UserModel user = session.users().getUserByUsername(realm, membership.getUsername());
        if (user == null) {
            return null;
        }

        if (!provider.isMember(organization, user)) {
            return null;
        }

        return organization.getId() + ":" + user.getId();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, OrganizationMembershipRepresentation membership) {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        var organization = provider.getById(membership.getOrganizationId());
        if (organization == null) {
            return false;
        }

        UserModel user = session.users().getUserByUsername(realm, membership.getUsername());
        if (user == null) {
            return false;
        }

        return provider.isMember(organization, user);
    }

    @Override
    public String existsMessage(RealmModel realm, OrganizationMembershipRepresentation membership) {
        return "Organization membership for user '" + membership.getUsername() +
               "' to organization '" + membership.getOrganizationId() + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.ORGANIZATION_MEMBERSHIP;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, OrganizationMembershipRepresentation membership) {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        var organization = provider.getById(membership.getOrganizationId());
        if (organization == null) {
            // Organization doesn't exist - skip removal silently
            return;
        }

        UserModel user = session.users().getUserByUsername(realm, membership.getUsername());
        if (user == null) {
            // User doesn't exist - skip removal silently
            return;
        }

        if (provider.isMember(organization, user)) {
            provider.removeMember(organization, user);
        }
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, OrganizationMembershipRepresentation membership) {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        if (provider == null) {
            throw new RuntimeException("OrganizationProvider is not available. Ensure the organization feature is enabled.");
        }

        var organization = provider.getById(membership.getOrganizationId());
        if (organization == null) {
            throw new RuntimeException("Organization with ID '" + membership.getOrganizationId() + "' not found");
        }

        UserModel user = session.users().getUserByUsername(realm, membership.getUsername());
        if (user == null) {
            throw new RuntimeException("User with username '" + membership.getUsername() + "' not found");
        }

        // Add the member with the appropriate membership type
        if (MembershipType.MANAGED.equals(membership.getMembershipType())) {
            provider.addManagedMember(organization, user);
        } else {
            provider.addMember(organization, user);
        }

        // Cache the created ID
        String key = makeKey(membership);
        String modelId = organization.getId() + ":" + user.getId();
        createdIds.put(key, modelId);
    }

}
