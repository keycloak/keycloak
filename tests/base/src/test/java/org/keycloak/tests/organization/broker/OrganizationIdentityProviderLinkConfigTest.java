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

package org.keycloak.tests.organization.broker;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationIdentityProviderLinkModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationIdentityProviderLinkConfigTest extends AbstractOrganizationTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testC2ValidationAutoMembershipFalseWithManagedRejected() {
        String orgId = createOrganization().getId();
        String idpAlias = organizationName + "-identity-provider";

        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = provider.getById(orgId);
            IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);

            // remove the default link to test addIdentityProvider with explicit config
            provider.removeIdentityProvider(org, idp);

            // AM=false + MANAGED must be rejected on add
            assertThrows(ModelValidationException.class, () ->
                    provider.addIdentityProvider(org, idp, false, MembershipType.MANAGED));

            // add with valid config, then test update path
            assertTrue(provider.addIdentityProvider(org, idp, true, MembershipType.UNMANAGED));

            // AM=false + MANAGED must be rejected on update
            assertThrows(ModelValidationException.class, () ->
                    provider.updateIdentityProviderLink(org, idp, false, MembershipType.MANAGED));
        });
    }

    @Test
    public void testC1ValidationAtMostOneManagedPerIdp() {
        OrganizationRepresentation orgA = createOrganization("orga", "orga.com");
        OrganizationRepresentation orgB = createOrganization("orgb", "orgb.com");

        // link orgA's IdP to orgB via REST
        String idpAlias = realm.admin().organizations().get(orgA.getId())
                .identityProviders().getIdentityProviders().get(0).getAlias();
        try (Response response = realm.admin().organizations().get(orgB.getId())
                .identityProviders().addIdentityProvider(idpAlias)) {
            assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
        }

        String orgAId = orgA.getId();
        String orgBId = orgB.getId();

        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orgAModel = provider.getById(orgAId);
            OrganizationModel orgBModel = provider.getById(orgBId);
            IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);

            // remove both default links to start clean
            provider.removeIdentityProvider(orgAModel, idp);
            provider.removeIdentityProvider(orgBModel, idp);

            // first org with MANAGED succeeds
            assertTrue(provider.addIdentityProvider(orgAModel, idp, true, MembershipType.MANAGED));

            // second org with MANAGED must be rejected
            assertThrows(ModelValidationException.class, () ->
                    provider.addIdentityProvider(orgBModel, idp, true, MembershipType.MANAGED));
        });
    }

    @Test
    public void testValidCombinations() {
        String orgId = createOrganization().getId();
        String idpAlias = organizationName + "-identity-provider";

        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = provider.getById(orgId);
            IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);

            // remove default link
            provider.removeIdentityProvider(org, idp);

            // AM=true, MT=UNMANAGED (default)
            assertTrue(provider.addIdentityProvider(org, idp, true, MembershipType.UNMANAGED));
            OrganizationIdentityProviderLinkModel link = provider.getIdentityProviderLink(org, idp);
            assertTrue(link.isAutoMembership());
            assertEquals(MembershipType.UNMANAGED, link.getMembershipType());
            provider.removeIdentityProvider(org, idp);

            // AM=true, MT=MANAGED
            assertTrue(provider.addIdentityProvider(org, idp, true, MembershipType.MANAGED));
            link = provider.getIdentityProviderLink(org, idp);
            assertTrue(link.isAutoMembership());
            assertEquals(MembershipType.MANAGED, link.getMembershipType());
            provider.removeIdentityProvider(org, idp);

            // AM=false, MT=UNMANAGED
            assertTrue(provider.addIdentityProvider(org, idp, false, MembershipType.UNMANAGED));
            link = provider.getIdentityProviderLink(org, idp);
            assertFalse(link.isAutoMembership());
            assertEquals(MembershipType.UNMANAGED, link.getMembershipType());
        });
    }

    @Test
    public void testGetIdentityProviderLink() {
        String orgId = createOrganization().getId();
        String idpAlias = organizationName + "-identity-provider";

        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = provider.getById(orgId);
            IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);

            // default link exists with AM=true, MT=UNMANAGED
            OrganizationIdentityProviderLinkModel link = provider.getIdentityProviderLink(org, idp);
            assertNotNull(link);
            assertTrue(link.isAutoMembership());
            assertEquals(MembershipType.UNMANAGED, link.getMembershipType());
            assertEquals(idp.getInternalId(), link.getIdentityProviderId());

            // remove link — should return null
            provider.removeIdentityProvider(org, idp);
            assertNull(provider.getIdentityProviderLink(org, idp));

            // add with specific config and verify
            assertTrue(provider.addIdentityProvider(org, idp, true, MembershipType.MANAGED));
            link = provider.getIdentityProviderLink(org, idp);
            assertNotNull(link);
            assertTrue(link.isAutoMembership());
            assertEquals(MembershipType.MANAGED, link.getMembershipType());
        });
    }

    @Test
    public void testUpdateIdentityProviderLink() {
        String orgId = createOrganization().getId();
        String idpAlias = organizationName + "-identity-provider";

        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel org = provider.getById(orgId);
            IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);

            // default link: AM=true, MT=UNMANAGED — update to AM=false, MT=UNMANAGED
            provider.updateIdentityProviderLink(org, idp, false, MembershipType.UNMANAGED);
            OrganizationIdentityProviderLinkModel link = provider.getIdentityProviderLink(org, idp);
            assertFalse(link.isAutoMembership());
            assertEquals(MembershipType.UNMANAGED, link.getMembershipType());

            // update to AM=true, MT=MANAGED
            provider.updateIdentityProviderLink(org, idp, true, MembershipType.MANAGED);
            link = provider.getIdentityProviderLink(org, idp);
            assertTrue(link.isAutoMembership());
            assertEquals(MembershipType.MANAGED, link.getMembershipType());

            // remove link, then update should throw
            provider.removeIdentityProvider(org, idp);
            assertThrows(ModelException.class, () ->
                    provider.updateIdentityProviderLink(org, idp, true, MembershipType.UNMANAGED));
        });
    }

    @Test
    public void testC1OnUpdatePath() {
        OrganizationRepresentation orgA = createOrganization("orga", "orga.com");
        OrganizationRepresentation orgB = createOrganization("orgb", "orgb.com");

        // link orgA's IdP to orgB via REST
        String idpAlias = realm.admin().organizations().get(orgA.getId())
                .identityProviders().getIdentityProviders().get(0).getAlias();
        try (Response response = realm.admin().organizations().get(orgB.getId())
                .identityProviders().addIdentityProvider(idpAlias)) {
            assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
        }

        String orgAId = orgA.getId();
        String orgBId = orgB.getId();

        runOnServer.run(session -> {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            OrganizationModel orgAModel = provider.getById(orgAId);
            OrganizationModel orgBModel = provider.getById(orgBId);
            IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);

            // both start with default AM=true, MT=UNMANAGED
            // update orgA to MANAGED — should succeed
            provider.updateIdentityProviderLink(orgAModel, idp, true, MembershipType.MANAGED);

            // update orgB to MANAGED — should fail (C1 violation)
            assertThrows(ModelValidationException.class, () ->
                    provider.updateIdentityProviderLink(orgBModel, idp, true, MembershipType.MANAGED));

            // revert orgA to UNMANAGED — releases the MANAGED slot
            provider.updateIdentityProviderLink(orgAModel, idp, true, MembershipType.UNMANAGED);

            // now orgB can become MANAGED
            provider.updateIdentityProviderLink(orgBModel, idp, true, MembershipType.MANAGED);
            OrganizationIdentityProviderLinkModel link = provider.getIdentityProviderLink(orgBModel, idp);
            assertEquals(MembershipType.MANAGED, link.getMembershipType());
        });
    }
}
