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

package org.keycloak.tests.organization.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.OrganizationIdentityProviderResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationIdentityProviderLinkRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

@KeycloakIntegrationTest
public class OrganizationIdentityProviderLinkTest extends AbstractOrganizationTest {

    @Test
    public void testDefaultLinkConfig() {
        OrganizationRepresentation org = createOrganization("test-default");
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());

        IdentityProviderRepresentation idpRep = orgResource.identityProviders()
                .get("test-default-identity-provider").toRepresentation();

        assertNotNull(idpRep.getAutoMembership());
        assertTrue(idpRep.getAutoMembership());
        assertEquals("UNMANAGED", idpRep.getOrgMembershipType());
    }

    @Test
    public void testUpdateLinkConfig() {
        OrganizationRepresentation org = createOrganization("test-update");
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());
        OrganizationIdentityProviderResource idpResource = orgResource.identityProviders()
                .get("test-update-identity-provider");

        OrganizationIdentityProviderLinkRepresentation linkRep = new OrganizationIdentityProviderLinkRepresentation();
        linkRep.setAutoMembership(true);
        linkRep.setMembershipType("MANAGED");

        try (Response response = idpResource.update(linkRep)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        IdentityProviderRepresentation updated = idpResource.toRepresentation();
        assertTrue(updated.getAutoMembership());
        assertEquals("MANAGED", updated.getOrgMembershipType());
    }

    @Test
    public void testUpdateToUnmanaged() {
        OrganizationRepresentation org = createOrganization("test-unmanaged");
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());
        OrganizationIdentityProviderResource idpResource = orgResource.identityProviders()
                .get("test-unmanaged-identity-provider");

        OrganizationIdentityProviderLinkRepresentation linkRep = new OrganizationIdentityProviderLinkRepresentation();
        linkRep.setAutoMembership(false);
        linkRep.setMembershipType("UNMANAGED");

        try (Response response = idpResource.update(linkRep)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        IdentityProviderRepresentation updated = idpResource.toRepresentation();
        assertEquals(false, updated.getAutoMembership());
        assertEquals("UNMANAGED", updated.getOrgMembershipType());
    }

    @Test
    public void testRejectAmFalseWithManaged() {
        OrganizationRepresentation org = createOrganization("test-c2");
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());
        OrganizationIdentityProviderResource idpResource = orgResource.identityProviders()
                .get("test-c2-identity-provider");

        OrganizationIdentityProviderLinkRepresentation linkRep = new OrganizationIdentityProviderLinkRepresentation();
        linkRep.setAutoMembership(false);
        linkRep.setMembershipType("MANAGED");

        try (Response response = idpResource.update(linkRep)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        IdentityProviderRepresentation unchanged = idpResource.toRepresentation();
        assertTrue(unchanged.getAutoMembership());
        assertEquals("UNMANAGED", unchanged.getOrgMembershipType());
    }

    @Test
    public void testRejectDuplicateManaged() {
        OrganizationRepresentation orgA = createOrganization("test-c1-a");
        OrganizationRepresentation orgB = createOrganization("test-c1-b");

        // Create a shared IdP and link it to both orgs
        IdentityProviderRepresentation sharedIdp = createOrgBroker("shared-c1");
        realm.admin().identityProviders().create(sharedIdp).close();
        realm.cleanup().add(r -> {
            try {
                r.identityProviders().get("shared-c1-identity-provider").remove();
            } catch (NotFoundException ignored) {}
        });

        // Link shared IdP to both orgs (need to unlink per-org brokers first for this shared one)
        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());

        orgAResource.identityProviders().addIdentityProvider("shared-c1-identity-provider").close();
        orgBResource.identityProviders().addIdentityProvider("shared-c1-identity-provider").close();

        // Set orgA's link to MANAGED
        OrganizationIdentityProviderLinkRepresentation linkRep = new OrganizationIdentityProviderLinkRepresentation();
        linkRep.setAutoMembership(true);
        linkRep.setMembershipType("MANAGED");

        OrganizationIdentityProviderResource orgAIdpResource = orgAResource.identityProviders()
                .get("shared-c1-identity-provider");
        try (Response response = orgAIdpResource.update(linkRep)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Try to set orgB's link to MANAGED too — should fail (C1)
        OrganizationIdentityProviderResource orgBIdpResource = orgBResource.identityProviders()
                .get("shared-c1-identity-provider");
        try (Response response = orgBIdpResource.update(linkRep)) {
            assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testAllowUnmanagedOnMultipleOrgs() {
        OrganizationRepresentation orgA = createOrganization("test-multi-a");
        OrganizationRepresentation orgB = createOrganization("test-multi-b");

        IdentityProviderRepresentation sharedIdp = createOrgBroker("shared-multi");
        realm.admin().identityProviders().create(sharedIdp).close();
        realm.cleanup().add(r -> {
            try {
                r.identityProviders().get("shared-multi-identity-provider").remove();
            } catch (NotFoundException ignored) {}
        });

        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());

        orgAResource.identityProviders().addIdentityProvider("shared-multi-identity-provider").close();
        orgBResource.identityProviders().addIdentityProvider("shared-multi-identity-provider").close();

        OrganizationIdentityProviderLinkRepresentation linkRep = new OrganizationIdentityProviderLinkRepresentation();
        linkRep.setAutoMembership(true);
        linkRep.setMembershipType("UNMANAGED");

        try (Response responseA = orgAResource.identityProviders().get("shared-multi-identity-provider").update(linkRep)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), responseA.getStatus());
        }

        try (Response responseB = orgBResource.identityProviders().get("shared-multi-identity-provider").update(linkRep)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), responseB.getStatus());
        }
    }

    @Test
    public void testListIncludesLinkConfig() {
        OrganizationRepresentation org = createOrganization("test-list");
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());

        // Update the link to MANAGED
        OrganizationIdentityProviderLinkRepresentation linkRep = new OrganizationIdentityProviderLinkRepresentation();
        linkRep.setAutoMembership(true);
        linkRep.setMembershipType("MANAGED");
        orgResource.identityProviders().get("test-list-identity-provider").update(linkRep).close();

        List<IdentityProviderRepresentation> idps = orgResource.identityProviders().getIdentityProviders();

        IdentityProviderRepresentation found = idps.stream()
                .filter(idp -> "test-list-identity-provider".equals(idp.getAlias()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("IdP not found in list"));

        assertTrue(found.getAutoMembership());
        assertEquals("MANAGED", found.getOrgMembershipType());
    }

    @Test
    public void testUpdateNonLinkedIdp() {
        OrganizationRepresentation org = createOrganization("test-nolink");

        // Create an IdP that is NOT linked to the org
        IdentityProviderRepresentation unlinkedIdp = createOrgBroker("unlinked");
        realm.admin().identityProviders().create(unlinkedIdp).close();
        realm.cleanup().add(r -> {
            try {
                r.identityProviders().get("unlinked-identity-provider").remove();
            } catch (NotFoundException ignored) {}
        });

        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());

        OrganizationIdentityProviderLinkRepresentation linkRep = new OrganizationIdentityProviderLinkRepresentation();
        linkRep.setAutoMembership(true);
        linkRep.setMembershipType("UNMANAGED");

        try (Response response = orgResource.identityProviders().get("unlinked-identity-provider").update(linkRep)) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }
}
