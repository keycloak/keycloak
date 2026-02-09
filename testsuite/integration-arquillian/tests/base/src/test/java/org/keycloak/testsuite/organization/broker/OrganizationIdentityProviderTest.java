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

import jakarta.ws.rs.BadRequestException;
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
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.models.OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class OrganizationIdentityProviderTest extends AbstractOrganizationTest {

    @Test
    public void testUpdate() {
        OrganizationRepresentation organization = createOrganization();
        OrganizationIdentityProviderResource orgIdPResource = testRealm().organizations().get(organization.getId())
                .identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation expected = orgIdPResource.toRepresentation();

        // organization link set
        Assert.assertEquals(expected.getOrganizationId(), organization.getId());

        IdentityProviderResource idpResource = testRealm().identityProviders().get(expected.getAlias());
        IdentityProviderRepresentation actual = idpResource.toRepresentation();
        Assert.assertEquals(actual.getOrganizationId(), organization.getId());
        actual.setOrganizationId("somethingelse");
        try {
            idpResource.update(actual);
            Assert.fail("Should fail because it maps to an invalid org");
        } catch (BadRequestException ignore) {
        }

        OrganizationRepresentation secondOrg = createOrganization("secondorg");
        actual.setOrganizationId(secondOrg.getId());
        idpResource.update(actual);
        actual = idpResource.toRepresentation();
        Assert.assertEquals(actual.getOrganizationId(), organization.getId());

        actual = idpResource.toRepresentation();
        // the link to the organization should not change
        Assert.assertEquals(actual.getOrganizationId(), organization.getId());
        actual.setOrganizationId(null);
        idpResource.update(actual);
        actual = idpResource.toRepresentation();
        // the link to the organization should not change
        Assert.assertEquals(actual.getOrganizationId(), organization.getId());

        String domain = actual.getConfig().get(ORGANIZATION_DOMAIN_ATTRIBUTE);

        assertNotNull(domain);
        actual.getConfig().put(ORGANIZATION_DOMAIN_ATTRIBUTE, " ");
        idpResource.update(actual);
        actual = idpResource.toRepresentation();
        // domain removed
        Assert.assertNull(actual.getConfig().get(ORGANIZATION_DOMAIN_ATTRIBUTE));

        actual.getConfig().put(ORGANIZATION_DOMAIN_ATTRIBUTE, domain);
        idpResource.update(actual);
        actual = idpResource.toRepresentation();
        // domain set again
        Assert.assertNotNull(actual.getConfig().get(ORGANIZATION_DOMAIN_ATTRIBUTE));

        actual.getConfig().remove(ORGANIZATION_DOMAIN_ATTRIBUTE);
        idpResource.update(actual);
        actual = idpResource.toRepresentation();
        // domain removed
        Assert.assertNull(actual.getConfig().get(ORGANIZATION_DOMAIN_ATTRIBUTE));
    }

    @Test
    public void testDelete() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation idpTemplate = organization
                .identityProviders().get(bc.getIDPAlias()).toRepresentation();

        //remove Org related stuff from the template
        idpTemplate.setOrganizationId(null);
        idpTemplate.getConfig().remove(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        idpTemplate.getConfig().remove(OrganizationModel.IdentityProviderRedirectMode.EMAIL_MATCH.getKey());

        for (int i = 0; i < 5; i++) {
            idpTemplate.setAlias("idp-" + i);
            idpTemplate.setInternalId(null);
            try (Response response = testRealm().identityProviders().create(idpTemplate)) {
                assertThat("Failed to create idp-" + i, response.getStatus(), equalTo(Status.CREATED.getStatusCode()));
            }
            try (Response response = organization.identityProviders().addIdentityProvider(idpTemplate.getAlias())) {
                assertThat("Failed to add idp-" + i, response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
            }
        }

        Assert.assertEquals(6, organization.identityProviders().getIdentityProviders().size());

        for (int i = 0; i < 5; i++) {
            String alias = "idp-" + i;
            OrganizationIdentityProviderResource idpResource = organization.identityProviders().get(alias);

            try (Response response = idpResource.delete()) {
                assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
            }

            try {
                idpResource.toRepresentation();
                Assert.fail("should be removed");
            } catch (NotFoundException expected) {
            }

            // not removed from the realm
            testRealm().identityProviders().get(alias).toRepresentation();
        }

        organization.identityProviders().get(bc.getIDPAlias()).delete().close();
        Assert.assertFalse(testRealm().identityProviders().findAll().isEmpty());
    }

    @Test
    public void testCreatingExistingIdentityProvider() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        OrganizationIdentityProviderResource orgIdPResource = organization
                .identityProviders().get(bc.getIDPAlias());

        IdentityProviderRepresentation idpRepresentation = orgIdPResource.toRepresentation();

        String alias = idpRepresentation.getAlias();
        idpRepresentation.setAlias("another-idp");
        testRealm().identityProviders().create(idpRepresentation).close();

        try (Response response = organization.identityProviders().addIdentityProvider(alias)) {
            // already associated with the org
            assertThat(response.getStatus(), equalTo(Status.CONFLICT.getStatusCode()));
        }

        idpRepresentation.setAlias(alias);
        idpRepresentation.setInternalId(null);

        OrganizationResource secondOrg = testRealm().organizations().get(createOrganization("secondorg").getId());

        try (Response response = secondOrg.identityProviders().addIdentityProvider(alias)) {
            // associated with another org
            assertThat(response.getStatus(), equalTo(Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test
    public void testRemovingOrgShouldRemoveIdP() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        try (Response response = orgResource.delete()) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
        }

        // broker not removed from realm
        IdentityProviderRepresentation idpRep = testRealm().identityProviders().get(bc.getIDPAlias()).toRepresentation();
        // broker no longer linked to the org
        Assert.assertNull(idpRep.getOrganizationId());
        Assert.assertNull(idpRep.getConfig().get(ORGANIZATION_DOMAIN_ATTRIBUTE));
    }

    @Test
    public void testUpdateOrDeleteIdentityProviderNotAssignedToOrganization() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());
        OrganizationIdentityProviderResource orgIdPResource = orgResource.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation idpRepresentation = createRep("some-broker", "oidc");
        getCleanup().addCleanup(() -> testRealm().identityProviders().get(idpRepresentation.getAlias()).remove());
        //create IdP in realm not bound to Org
        testRealm().identityProviders().create(idpRepresentation).close();

        try (Response response = orgIdPResource.delete()) {
            assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
        }

        try (Response response = orgIdPResource.delete()) {
            assertThat(response.getStatus(), equalTo(Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    public void testAssignDomainNotBoundToOrganization() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());
        OrganizationIdentityProviderResource orgIdPResource = orgResource.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation idpRep = orgIdPResource.toRepresentation();
        idpRep.getConfig().put(ORGANIZATION_DOMAIN_ATTRIBUTE, "unknown.org");

        try {
            testRealm().identityProviders().get(idpRep.getAlias()).update(idpRep);
            Assert.fail("Domain set to broker is invalid");
        } catch (BadRequestException ignore) {

        }

        idpRep.setAlias("newbroker");
        idpRep.setInternalId(null);
        try (Response response = testRealm().identityProviders().create(idpRep)) {
            Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
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
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());
        OrganizationIdentityProviderResource orgIdPResource = orgResource.identityProviders().get("testorg-identity-provider");
        IdentityProviderRepresentation idpRep = orgIdPResource.toRepresentation();

        // IDP should have been assigned to the first domain.
        assertThat(idpRep.getConfig().get(ORGANIZATION_DOMAIN_ATTRIBUTE), is(equalTo("testorg.com")));

        // let's update the organization, removing the domain linked to the IDP.
        orgRep.removeDomain(orgRep.getDomain("testorg.com"));
        try (Response response = orgResource.update(orgRep)) {
            assertThat(response.getStatus(), is(equalTo(Status.NO_CONTENT.getStatusCode())));
        }

        // fetch the idp config and check if the domain has been unlinked.
        idpRep = orgIdPResource.toRepresentation();
        assertThat(idpRep.getConfig().get(ORGANIZATION_DOMAIN_ATTRIBUTE), is(nullValue()));
    }

    @Test
    public void testLinkIdentityProviderToOrganizationWithoutDomain() {
        OrganizationRepresentation orgRep = createOrganization("myorg", new String[0]);
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());
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
