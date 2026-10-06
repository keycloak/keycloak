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

package org.keycloak.tests.organization.authz.fgap;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KeycloakIntegrationTest
public class OrganizationScopeFgapTest {

    private static final String MEMBER_USERNAME = "orguser";
    private static final String MEMBER_PASSWORD = "password";

    @InjectRealm(config = OrganizationFgapConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    private String memberId;

    @BeforeEach
    public void setup() {
        String orgAId = createOrg("o1", "o1.org");
        String orgBId = createOrg("o2", "o2.org");

        realm.admin().clients().create(ClientBuilder.create("direct-grant")
                .secret("password")
                .directAccessGrantsEnabled()
                .redirectUris("*")
                .build()).close();

        UserRepresentation memberRep = UserBuilder.create()
                .username(MEMBER_USERNAME)
                .name("Org", "User")
                .email(MEMBER_USERNAME + "@o1.org")
                .emailVerified(true)
                .password(MEMBER_PASSWORD)
                .build();
        String memberId;
        try (Response response = realm.admin().users().create(memberRep)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            memberId = ApiUtil.getCreatedId(response);
        }

        realm.admin().organizations().get(orgAId).members().addMember(memberId).close();
        realm.admin().organizations().get(orgBId).members().addMember(memberId).close();

        this.memberId = memberId;

        oauth.client("direct-grant", "password");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSpecificOrganizationScope() throws Exception {
        grantQueryClientsRole();
        oauth.scope("openid organization:o1");

        AccessTokenResponse response = oauth.doPasswordGrantRequest(MEMBER_USERNAME, MEMBER_PASSWORD);
        assertThat(response.getStatusCode(), equalTo(Response.Status.OK.getStatusCode()));

        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, notNullValue());
        assertThat(organizations.contains("o1"), is(true));
        assertThat(organizations.contains("o2"), is(false));

        // refreshing exercises TokenManager#validateSelectedOrganization and the refresh branch of
        // OrganizationTokenPostProcessor, both of which resolve the organization by alias
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertThat(response.getStatusCode(), equalTo(Response.Status.OK.getStatusCode()));

        accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, notNullValue());
        assertThat(organizations.contains("o1"), is(true));
        assertThat(organizations.contains("o2"), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllOrganizationsScope() throws Exception {
        grantQueryClientsRole();
        oauth.scope("openid organization:*");

        AccessTokenResponse response = oauth.doPasswordGrantRequest(MEMBER_USERNAME, MEMBER_PASSWORD);
        assertThat(response.getStatusCode(), equalTo(Response.Status.OK.getStatusCode()));

        AccessToken accessToken = TokenVerifier.create(response.getAccessToken(), AccessToken.class).getToken();
        assertThat(accessToken.getOtherClaims().keySet(), hasItem(OAuth2Constants.ORGANIZATION));

        List<String> organizations = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertThat(organizations, notNullValue());
        assertThat(organizations.contains("o1"), is(true));
        assertThat(organizations.contains("o2"), is(true));
    }

    /**
     * Grants an admin role unrelated to organizations. It is enough to make the ORGANIZATIONS resource type
     * subject to partial evaluation while the very same user is being authenticated.
     */
    private void grantQueryClientsRole() {
        ClientResource realmManagement = AdminApiUtil.findClientByClientId(realm.admin(), Constants.REALM_MANAGEMENT_CLIENT_ID);
        RoleRepresentation queryClients = realmManagement.roles().get(AdminRoles.QUERY_CLIENTS).toRepresentation();
        realm.admin().users().get(memberId).roles()
                .clientLevel(realmManagement.toRepresentation().getId())
                .add(List.of(queryClients));
    }

    private String createOrg(String name, String domainName) {
        OrganizationRepresentation orgRep = new OrganizationRepresentation();
        orgRep.setName(name);
        orgRep.setAlias(name);
        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName(domainName);
        orgRep.addDomain(domain);

        try (Response response = realm.admin().organizations().create(orgRep)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            return ApiUtil.getCreatedId(response);
        }
    }
}
