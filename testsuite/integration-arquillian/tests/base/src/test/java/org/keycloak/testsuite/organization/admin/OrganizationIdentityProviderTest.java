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
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationIdentityProviderResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.Profile.Feature;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

@EnableFeature(Feature.ORGANIZATION)
public class OrganizationIdentityProviderTest extends AbstractOrganizationTest {

    private final String idpAlias = "org-identity-provider";

    @Before
    public void addCleanups() {
        addCleanupIdP(idpAlias);
    }

    @Test
    public void testCRUD() {
        OrganizationIdentityProviderResource orgIdPResource = testRealm().organizations().get(createOrganization().getId()).identityProvider();

        //create, read
        IdentityProviderRepresentation idpRepresentation = createRep(idpAlias, "oidc");
        try (Response response = orgIdPResource.create(idpRepresentation)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }
        idpRepresentation = orgIdPResource.toRepresentation();
        assertThat(idpRepresentation.getAlias(), equalTo(idpAlias));

        String updatedIdpAlias = "updated-org-identity-provider";
        //update
        idpRepresentation.setAlias(updatedIdpAlias);
        try (Response response = orgIdPResource.update(idpRepresentation)) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
            addCleanupIdP(updatedIdpAlias);
        }
        assertThat(orgIdPResource.toRepresentation().getAlias(), equalTo(updatedIdpAlias));

        //delete
        try (Response response = orgIdPResource.delete()) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
        }
        assertThat(orgIdPResource.toRepresentation(), nullValue());
    }

    @Test
    public void tryCreateSecondIdp() {
        OrganizationIdentityProviderResource orgIdPResource = testRealm().organizations().get(createOrganization().getId()).identityProvider();

        IdentityProviderRepresentation idpRepresentation = createRep(idpAlias, "oidc");
        try (Response response = orgIdPResource.create(idpRepresentation)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }

        idpRepresentation.setAlias("another-idp");
        try (Response response = orgIdPResource.create(idpRepresentation)) {
            assertThat(response.getStatus(), equalTo(Response.Status.BAD_REQUEST.getStatusCode()));
        }
    }

    @Test(expected = jakarta.ws.rs.NotFoundException.class)
    public void removingOrgShouldRemoveIdP() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        OrganizationIdentityProviderResource orgIdPResource = orgResource.identityProvider();

        IdentityProviderRepresentation idpRepresentation = createRep(idpAlias, "oidc");
        try (Response response = orgIdPResource.create(idpRepresentation)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
        }

        try (Response response = orgResource.delete()) {
            assertThat(response.getStatus(), equalTo(Response.Status.NO_CONTENT.getStatusCode()));
        }

        testRealm().identityProviders().get(idpAlias).toRepresentation();
    }

    @Test
    public void tryUpdateAndRemoveIdPNotAssignedToOrg() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource orgResource = testRealm().organizations().get(orgRep.getId());

        OrganizationIdentityProviderResource orgIdPResource = orgResource.identityProvider();

        IdentityProviderRepresentation idpRepresentation = createRep(idpAlias, "oidc");
        //create IdP in realm not bound to Org
        testRealm().identityProviders().create(idpRepresentation).close();

        try (Response response = orgIdPResource.update(idpRepresentation)) {
            assertThat(response.getStatus(), equalTo(Response.Status.NOT_FOUND.getStatusCode()));
        }
        try (Response response = orgIdPResource.delete()) {
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

    private void addCleanupIdP(String alias) {
        getCleanup().addCleanup(() -> testRealm().identityProviders().get(alias).remove());
    }
}
