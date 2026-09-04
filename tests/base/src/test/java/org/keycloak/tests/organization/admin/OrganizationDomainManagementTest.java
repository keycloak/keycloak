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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.OrganizationDomainLinksResource;
import org.keycloak.admin.client.resource.OrganizationDomainsResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

@KeycloakIntegrationTest
public class OrganizationDomainManagementTest extends AbstractOrganizationTest {

    // -------- Realm-level domain CRUD --------

    @Test
    public void testCreateAndGetDomain() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName("standalone.example.com");
        rep.setVerified(true);
        rep.setAutoRedirect(false);

        try (Response response = domains.create(rep)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }
        addDomainCleanup("standalone.example.com");

        OrganizationDomainRepresentation fetched = domains.get("standalone.example.com");
        assertNotNull(fetched);
        assertEquals("standalone.example.com", fetched.getName());
        assertTrue(fetched.isVerified());
    }

    @Test
    public void testCreateDuplicateDomain() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName("dup.example.com");

        try (Response response = domains.create(rep)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }
        addDomainCleanup("dup.example.com");

        try (Response response = domains.create(rep)) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testListDomains() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        for (String name : List.of("list-a.example.com", "list-b.example.com")) {
            OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
            rep.setName(name);
            domains.create(rep).close();
            addDomainCleanup(name);
        }

        List<OrganizationDomainRepresentation> all = domains.search("list-", null, null);
        assertTrue(all.size() >= 2, "Should find at least 2 domains matching 'list-'");
    }

    @Test
    public void testUpdateDomain() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName("update.example.com");
        rep.setVerified(false);
        domains.create(rep).close();
        addDomainCleanup("update.example.com");

        OrganizationDomainRepresentation updateRep = new OrganizationDomainRepresentation();
        updateRep.setVerified(true);
        updateRep.setAutoRedirect(true);

        try (Response response = domains.update("update.example.com", updateRep)) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        OrganizationDomainRepresentation fetched = domains.get("update.example.com");
        assertTrue(fetched.isVerified());
        assertTrue(fetched.isAutoRedirect());
    }

    @Test
    public void testDeleteUnclaimedDomain() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName("todelete.example.com");
        domains.create(rep).close();

        try (Response response = domains.delete("todelete.example.com")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        try (Response response = domains.delete("todelete.example.com")) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testDeleteClaimedDomainFails() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName("claimed-del.example.com");
        domains.create(rep).close();
        addDomainCleanup("claimed-del.example.com");

        OrganizationRepresentation org = createOrganization("del-claim-test", (String[]) null);
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());

        try (Response response = orgResource.domains().claim("claimed-del.example.com")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        try (Response response = domains.delete("claimed-del.example.com")) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    // -------- Per-org domain linking --------

    @Test
    public void testClaimDomain() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName("claim.example.com");
        rep.setVerified(true);
        domains.create(rep).close();
        addDomainCleanup("claim.example.com");

        OrganizationRepresentation org = createOrganization("claim-test", (String[]) null);
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());

        try (Response response = orgResource.domains().claim("claim.example.com")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        List<OrganizationDomainRepresentation> orgDomains = orgResource.domains().getAll();
        assertTrue(orgDomains.stream().anyMatch(d -> "claim.example.com".equals(d.getName())));

        OrganizationDomainRepresentation orgDomain = orgResource.domains().get("claim.example.com");
        assertEquals("claim.example.com", orgDomain.getName());
        assertTrue(orgDomain.isVerified());
    }

    @Test
    public void testClaimDomainAlreadyClaimed() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName("double-claim.example.com");
        domains.create(rep).close();
        addDomainCleanup("double-claim.example.com");

        OrganizationRepresentation org = createOrganization("double-claim-test", (String[]) null);
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());

        try (Response response = orgResource.domains().claim("double-claim.example.com")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        try (Response response = orgResource.domains().claim("double-claim.example.com")) {
            assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testClaimNonExistentDomain() {
        OrganizationRepresentation org = createOrganization("no-domain-test", (String[]) null);
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());

        try (Response response = orgResource.domains().claim("nonexistent.example.com")) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testUnclaimDomain() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName("unclaim.example.com");
        domains.create(rep).close();
        addDomainCleanup("unclaim.example.com");

        OrganizationRepresentation org = createOrganization("unclaim-test", (String[]) null);
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());

        orgResource.domains().claim("unclaim.example.com").close();

        try (Response response = orgResource.domains().unclaim("unclaim.example.com")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        List<OrganizationDomainRepresentation> orgDomains = orgResource.domains().getAll();
        assertTrue(orgDomains.stream().noneMatch(d -> "unclaim.example.com".equals(d.getName())));

        OrganizationDomainRepresentation realmDomain = domains.get("unclaim.example.com");
        assertNotNull(realmDomain, "Domain should still exist in realm after unclaiming");
    }

    @Test
    public void testDomainSharedAcrossOrgs() {
        OrganizationDomainsResource domains = realm.admin().organizations().domains();

        OrganizationDomainRepresentation rep = new OrganizationDomainRepresentation();
        rep.setName("shared.example.com");
        rep.setVerified(true);
        domains.create(rep).close();
        addDomainCleanup("shared.example.com");

        OrganizationRepresentation orgA = createOrganization("shared-a", (String[]) null);
        OrganizationRepresentation orgB = createOrganization("shared-b", (String[]) null);

        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());

        try (Response responseA = orgAResource.domains().claim("shared.example.com")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), responseA.getStatus());
        }
        try (Response responseB = orgBResource.domains().claim("shared.example.com")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), responseB.getStatus());
        }

        assertTrue(orgAResource.domains().getAll().stream().anyMatch(d -> "shared.example.com".equals(d.getName())));
        assertTrue(orgBResource.domains().getAll().stream().anyMatch(d -> "shared.example.com".equals(d.getName())));
    }

    // -------- Helpers --------

    private void addDomainCleanup(String domainName) {
        realm.cleanup().add(r -> {
            try {
                r.organizations().domains().delete(domainName).close();
            } catch (Exception ignored) {}
        });
    }
}
