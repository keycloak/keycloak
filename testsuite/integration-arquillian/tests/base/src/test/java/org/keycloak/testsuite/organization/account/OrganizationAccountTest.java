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

package org.keycloak.testsuite.organization.account;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.account.LinkedAccountRepresentation;
import org.keycloak.representations.account.OrganizationRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OrganizationAccountTest extends AbstractOrganizationTest {

    @Rule
    public TokenUtil tokenUtil = new TokenUtil(bc.getUserEmail(), bc.getUserPassword());

    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testFailUnlinkIdentityProvider() throws IOException {
        // federate user
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        assertBrokerRegistration(organization, bc.getUserLogin(), bc.getUserEmail());
        // reset password to obtain a token and access the account api
        UserRepresentation user = testRealm().users().searchByEmail(bc.getUserEmail(), true).get(0);
        ApiUtil.resetUserPassword(realmsResouce().realm(bc.consumerRealmName()).users().get(user.getId()), bc.getUserPassword(), false);

        LinkedAccountRepresentation link = findLinkedAccount(bc.getIDPAlias());
        Assert.assertNotNull(link);
        try (SimpleHttpResponse response = SimpleHttpDefault.doDelete(getAccountUrl("linked-accounts/" + link.getProviderAlias()), client).auth(tokenUtil.getToken()).acceptJson().asResponse()) {
            Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorRepresentation error = response.asJson(ErrorRepresentation.class);
            Assert.assertEquals("You cannot remove the link to an identity provider associated with an organization.", error.getErrorMessage());
        }

        // broker no longer linked to the organization
        organization.identityProviders().get(bc.getIDPAlias()).delete().close();
        try (SimpleHttpResponse response = SimpleHttpDefault.doDelete(getAccountUrl("linked-accounts/" + link.getProviderAlias()), client).auth(tokenUtil.getToken()).acceptJson().asResponse()) {
            Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGetOrganizations() throws Exception {
        UserRepresentation member = createUser();
        org.keycloak.representations.idm.OrganizationRepresentation orgA = createOrganization("orga");
        testRealm().organizations().get(orgA.getId()).members().addMember(member.getId()).close();
        org.keycloak.representations.idm.OrganizationRepresentation orgB = createOrganization("orgb");
        testRealm().organizations().get(orgB.getId()).members().addMember(member.getId()).close();

        List<OrganizationRepresentation> organizations = getOrganizations();
        Assert.assertEquals(2, organizations.size());
        OrganizationRepresentation organization = organizations.stream()
                .filter(o -> orgA.getId().equals(o.getId()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull(organization);
        Assert.assertEquals(orgA.getId(), organization.getId());
        Assert.assertEquals(orgA.getAlias(), organization.getAlias());
        Assert.assertEquals(orgA.getName(), organization.getName());
        Assert.assertEquals(orgA.getDescription(), organization.getDescription());
        Assert.assertEquals(orgA.getDomains().size(), organization.getDomains().size());
        Assert.assertTrue(organization.getDomains().containsAll(orgA.getDomains().stream().map(OrganizationDomainRepresentation::getName).toList()));
    }

    private SortedSet<LinkedAccountRepresentation> linkedAccountsRep() throws IOException {
        return SimpleHttpDefault.doGet(getAccountUrl("linked-accounts"), client).auth(tokenUtil.getToken())
                .asJson(new TypeReference<>() {});
    }

    private String getAccountUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account" + (resource != null ? "/" + resource : "");
    }

    private LinkedAccountRepresentation findLinkedAccount(String providerAlias) throws IOException {
        for (LinkedAccountRepresentation account : linkedAccountsRep()) {
            if (account.getProviderAlias().equals(providerAlias)) return account;
        }

        return null;
    }

    private List<OrganizationRepresentation> getOrganizations() throws IOException {
        return SimpleHttpDefault.doGet(getAccountUrl("organizations"), client).auth(tokenUtil.getToken())
                .asJson(new TypeReference<>() {});
    }

    private UserRepresentation createUser() {
        testRealm().users().create(UserBuilder.create()
                .username(bc.getUserEmail())
                .email(bc.getUserEmail())
                .password(bc.getUserPassword())
                .enabled(true)
                .build()).close();
        UserRepresentation member = testRealm().users().searchByEmail(bc.getUserEmail(), true).get(0);
        getCleanup().addUserId(member.getId());
        return member;
    }
}
