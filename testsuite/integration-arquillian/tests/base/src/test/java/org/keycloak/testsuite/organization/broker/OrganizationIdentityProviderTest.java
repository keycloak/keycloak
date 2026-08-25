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

package org.keycloak.testsuite.organization.broker;

import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.OrganizationIdentityProviderResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OrganizationIdentityProviderTest extends AbstractOrganizationTest {

    @Test
    public void testUpdate() {
        OrganizationRepresentation organization = createOrganization();
        OrganizationIdentityProviderResource orgIdPResource = managedRealm.admin().organizations().get(organization.getId())
                .identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation expected = orgIdPResource.toRepresentation();

        // organization link set
        Assertions.assertEquals(expected.getOrganizationId(), organization.getId());

        IdentityProviderResource idpResource = managedRealm.admin().identityProviders().get(expected.getAlias());
        IdentityProviderRepresentation actual = idpResource.toRepresentation();
        Assertions.assertEquals(actual.getOrganizationId(), organization.getId());
        // ignore organization id from repo when updating
        actual.setOrganizationId("somethingelse");
        idpResource.update(actual);
        actual = idpResource.toRepresentation();
        assertEquals(actual.getOrganizationId(), organization.getId());

        OrganizationRepresentation secondOrg = createOrganization("secondorg");
        actual.setOrganizationId(secondOrg.getId());
        idpResource.update(actual);
        actual = idpResource.toRepresentation();
        Assertions.assertEquals(actual.getOrganizationId(), organization.getId());

        actual = idpResource.toRepresentation();
        // the link to the organization should not change
        Assertions.assertEquals(actual.getOrganizationId(), organization.getId());
        actual.setOrganizationId(null);
        idpResource.update(actual);
        actual = idpResource.toRepresentation();
        // the link to the organization should not change
        Assertions.assertEquals(actual.getOrganizationId(), organization.getId());
    }

    @Test
    public void testDelete() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpTemplate = organization
                .identityProviders().get(bc.getIDPAlias()).toRepresentation();

        //remove Org related stuff from the template
        idpTemplate.setOrganizationId(null);

        for (int i = 0; i < 5; i++) {
            idpTemplate.setAlias("idp-" + i);
            idpTemplate.setInternalId(null);
            try (Response response = managedRealm.admin().identityProviders().create(idpTemplate)) {
                assertThat("Failed to create idp-" + i, response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }
            try (Response response = organization.identityProviders().addIdentityProvider(idpTemplate.getAlias())) {
                assertThat("Failed to add idp-" + i, response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }
        }

        Assertions.assertEquals(6, organization.identityProviders().getIdentityProviders().size());

        for (int i = 0; i < 5; i++) {
            String alias = "idp-" + i;
            OrganizationIdentityProviderResource idpResource = organization.identityProviders().get(alias);

            try (Response response = idpResource.delete()) {
                assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
            }

            try {
                idpResource.toRepresentation();
                Assertions.fail("should be removed");
            } catch (NotFoundException expected) {
            }

            // not removed from the realm
            managedRealm.admin().identityProviders().get(alias).toRepresentation();
        }

        organization.identityProviders().get(bc.getIDPAlias()).delete().close();
        Assertions.assertFalse(managedRealm.admin().identityProviders().findAll().isEmpty());
    }

    @Test
    public void testCreatingExistingIdentityProvider() {
        OrganizationResource organization = managedRealm.admin().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource orgIdPResource = organization
                .identityProviders().get(bc.getIDPAlias());

        IdentityProviderRepresentation idpRepresentation = orgIdPResource.toRepresentation();

        String alias = idpRepresentation.getAlias();
        idpRepresentation.setAlias("another-idp");
        managedRealm.admin().identityProviders().create(idpRepresentation).close();

        try (Response response = organization.identityProviders().addIdentityProvider(alias)) {
            // already associated with the org
            assertThat(response.getStatus(), equalTo(Status.CONFLICT.getStatusCode()));
        }

        idpRepresentation.setAlias(alias);
        idpRepresentation.setInternalId(null);

        OrganizationResource secondOrg = managedRealm.admin().organizations().get(createOrganization("secondorg").getId());

        try (Response response = secondOrg.identityProviders().addIdentityProvider(alias)) {
            // associated with another org
            assertThat(response.getStatus(), equalTo(Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testRemovingOrgShouldRemoveIdP() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = managedRealm.admin().organizations().get(orgRep.getId());

        try (Response response = orgResource.delete()) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
        }

        // broker not removed from realm
        IdentityProviderRepresentation idpRep = managedRealm.admin().identityProviders().get(bc.getIDPAlias()).toRepresentation();
        // broker no longer linked to the org
        Assertions.assertNull(idpRep.getOrganizationId());
    }

    @Test
    public void testUpdateOrDeleteIdentityProviderNotAssignedToOrganization() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = managedRealm.admin().organizations().get(orgRep.getId());
        OrganizationIdentityProviderResource orgIdPResource = orgResource.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation idpRepresentation = createRep("some-broker", "oidc");
        getCleanup().addCleanup(() -> managedRealm.admin().identityProviders().get(idpRepresentation.getAlias()).remove());
        //create IdP in realm not bound to Org
        managedRealm.admin().identityProviders().create(idpRepresentation).close();

        try (Response response = orgIdPResource.delete()) {
            assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
        }

        try (Response response = orgIdPResource.delete()) {
            assertThat(response.getStatus(), equalTo(Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    public void testAssignDomainWithNonExistentIdp() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = managedRealm.admin().organizations().get(orgRep.getId());

        orgRep = orgResource.toRepresentation();
        orgRep.getDomains().stream()
                .filter(d -> d.getName().equals(organizationName + ".org"))
                .findFirst()
                .ifPresent(d -> d.setIdentityProviderAlias("unknown-idp"));

        try (Response response = orgResource.update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.BAD_REQUEST.getStatusCode())));
        }
    }

    @Test
    public void testAddIdpFromDifferentRealm() {
        String orgId = createOrganization().getId();
        IdentityProviderRepresentation idpRepresentation = createRep("master-identity-provider", "oidc");
        adminClient.realm("master").identityProviders().create(idpRepresentation).close();

        try {
                getTestingClient().server(TEST_REALM_NAME).run(session -> {
                OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
                OrganizationModel organization = provider.getById(orgId);

                // adjust the session context to use the master realm to be able to retrieve the idp.
                RealmModel realm = session.realms().getRealmByName("master");
                RealmModel current = session.getContext().getRealm();
                session.getContext().setRealm(realm);
                IdentityProviderModel idp = session.identityProviders().getByAlias("master-identity-provider");

                // restore the context and try to add the idp.
                session.getContext().setRealm(current);
                assertFalse(provider.addIdentityProvider(organization, idp));
            });

        } finally {
            adminClient.realm("master").identityProviders().get("master-identity-provider").remove();
        }
    }

    @Test
    public void testRemovedDomainUpdatedInIDP() {
        OrganizationRepresentation orgRep = createOrganization("testorg", "testorg.com", "testorg.net");
        OrganizationResource orgResource = managedRealm.admin().organizations().get(orgRep.getId());

        // first domain should have the IdP assigned
        orgRep = orgResource.toRepresentation();
        OrganizationDomainRepresentation firstDomain = orgRep.getDomain("testorg.com");
        assertThat(firstDomain.getIdentityProviderAlias(), is(equalTo("testorg-identity-provider")));

        // remove the domain linked to the IDP
        orgRep.removeDomain(firstDomain);
        try (Response response = orgResource.update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        // remaining domain should not have an IdP assigned
        orgRep = orgResource.toRepresentation();
        OrganizationDomainRepresentation remainingDomain = orgRep.getDomain("testorg.net");
        assertNotNull(remainingDomain);
        assertThat(remainingDomain.getIdentityProviderAlias(), is(nullValue()));
    }

    @Test
    public void testLinkIdentityProviderToOrganizationWithoutDomain() {
        OrganizationRepresentation orgRep = createOrganization("myorg", new String[0]);
        OrganizationResource orgResource = managedRealm.admin().organizations().get(orgRep.getId());
        List<IdentityProviderRepresentation> identityProviders = orgResource.identityProviders().getIdentityProviders();
        assertThat(identityProviders.size(), is(1));
    }

    private IdentityProviderRepresentation createRep(String alias, String providerId) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();

        idp.setAlias(alias);
        idp.setDisplayName(alias);
        idp.setProviderId(providerId);
        idp.setEnabled(true);
        return idp;
    }
}
