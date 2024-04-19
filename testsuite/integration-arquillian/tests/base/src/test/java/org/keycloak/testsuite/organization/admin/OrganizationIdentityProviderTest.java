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

package org.keycloak.testsuite.organization.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationIdentityProviderResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationIdentityProviderTest extends AbstractOrganizationTest {

    @Test
    public void testUpdate() {
        OrganizationRepresentation organization = createOrganization();
        OrganizationIdentityProviderResource orgIdPResource = testRealm().organizations().get(organization.getId()).identityProvider();
        IdentityProviderRepresentation actual = orgIdPResource.toRepresentation();
        IdentityProviderRepresentation expected = actual;
        assertThat(expected.getAlias(), equalTo(bc.getIDPAlias()));

        //update
        expected.setDisplayName("My Org Broker");
        expected.getConfig().put("test", "value");
        try (Response response = orgIdPResource.update(expected)) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
        }
        actual = orgIdPResource.toRepresentation();
        assertThat(expected.getDisplayName(), equalTo(actual.getDisplayName()));
        Assert.assertEquals(expected.getConfig().get("test"), actual.getConfig().get("test"));
    }

    @Test
    public void testFailUpdateAlias() {
        OrganizationRepresentation organization = createOrganization();
        OrganizationIdentityProviderResource orgIdPResource = testRealm().organizations().get(organization.getId()).identityProvider();
        IdentityProviderRepresentation idpRepresentation = orgIdPResource.toRepresentation();
        assertThat(idpRepresentation.getAlias(), equalTo(bc.getIDPAlias()));

        //update
        idpRepresentation.setAlias("should-fail");
        try (Response response = orgIdPResource.update(idpRepresentation)) {
            assertThat(response.getStatus(), equalTo(Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    public void testDelete() {
        OrganizationRepresentation organization = createOrganization();
        OrganizationIdentityProviderResource orgIdPResource = testRealm().organizations().get(organization.getId()).identityProvider();

        try (Response response = orgIdPResource.delete()) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
        }
        assertThat(orgIdPResource.toRepresentation(), nullValue());
    }

    @Test
    public void tryCreateSecondIdp() {
        OrganizationIdentityProviderResource orgIdPResource = testRealm().organizations().get(createOrganization().getId()).identityProvider();

        IdentityProviderRepresentation idpRepresentation = orgIdPResource.toRepresentation();

        idpRepresentation.setAlias("another-idp");
        try (Response response = orgIdPResource.create(idpRepresentation)) {
            assertThat(response.getStatus(), equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test(expected = jakarta.ws.rs.NotFoundException.class)
    public void removingOrgShouldRemoveIdP() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        try (Response response = orgResource.delete()) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
        }

        testRealm().identityProviders().get(bc.getIDPAlias()).toRepresentation();
    }

    @Test
    public void tryUpdateAndRemoveIdPNotAssignedToOrg() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        OrganizationIdentityProviderResource orgIdPResource = orgResource.identityProvider();

        IdentityProviderRepresentation idpRepresentation = createRep("some-broker", "oidc");
        //create IdP in realm not bound to Org
        testRealm().identityProviders().create(idpRepresentation).close();

        try (Response response = orgIdPResource.update(idpRepresentation)) {
            assertThat(response.getStatus(), equalTo(Response.Status.NOT_FOUND.getStatusCode()));
        }
        try (Response response = orgIdPResource.delete()) {
            assertThat(response.getStatus(), equalTo(Status.NO_CONTENT.getStatusCode()));
        }
        try (Response response = orgIdPResource.delete()) {
            assertThat(response.getStatus(), equalTo(Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    public void tryUpdateIdPWithValidAliasInvalidInternalId() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        OrganizationIdentityProviderResource orgIdPResource = orgResource.identityProvider();

        IdentityProviderRepresentation idpRepresentation = createRep("some-broker", "oidc");
        //create IdP in realm not bound to Org and get created internalId
        testRealm().identityProviders().create(idpRepresentation).close();
        String internalId = testRealm().identityProviders().get("some-broker").toRepresentation().getInternalId();

        IdentityProviderRepresentation orgIdPRep = orgIdPResource.toRepresentation();
        orgIdPRep.setInternalId(internalId);

        try (Response response = orgIdPResource.update(orgIdPRep)) {
            assertThat(response.getStatus(), equalTo(Response.Status.NOT_FOUND.getStatusCode()));
        }
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
