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

import java.io.IOException;
import java.util.List;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.RegisterPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.MailUtils;

import org.junit.jupiter.api.Test;

import static org.keycloak.tests.organization.admin.AbstractOrganizationTest.IDP_ALIAS;
import static org.keycloak.tests.organization.admin.AbstractOrganizationTest.setUpOrgBroker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Invitation roles are granted when the invitee joins through an identity provider. The invitation link opens the
 * registration page, which has no identity provider buttons, so the invitee goes back to the login page and picks the
 * provider from there. The authentication session, and with it the invitation, survives that step.
 */
@KeycloakIntegrationTest
public class OrganizationInvitationRoleBrokerTest {

    private static final String ORG_NAME = "neworg";
    private static final String ORG_DOMAIN = "neworg.org";
    private static final String ROLE_NAME = "broker-invitation-role";

    @InjectRealm(ref = "provider", config = AbstractOrganizationTest.ProviderRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = AbstractOrganizationTest.OrganizationRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectUser(ref = "alice", realmRef = "provider", config = AbstractOrganizationTest.AliceUserConf.class)
    ManagedUser aliceFromProviderRealm;

    @InjectMailServer
    MailServer mailServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    RegisterPage registerPage;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void testInvitationRolesGrantedOnBrokerLogin() throws IOException {
        OrganizationResource organization = createOrganizationWithBroker();
        String roleId = createRealmRole();

        try (Response response = organization.members().inviteUser(aliceFromProviderRealm.getEmail(), "Alice", "Org", null, List.of(roleId))) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        driver.open(getInvitationLink());
        registerPage.assertCurrent();
        registerPage.clickBackToLogin();
        loginPage.clickSocial(IDP_ALIAS);

        assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"),
                "Picking the identity provider should lead to the provider realm");

        loginPage.fillLogin(aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getPassword());
        loginPage.submit();

        List<UserRepresentation> users = consumerRealm.admin().users().searchByEmail(aliceFromProviderRealm.getEmail(), true);
        assertEquals(1, users.size(), "Federated user should be created in the consumer realm");

        String userId = users.get(0).getId();
        consumerRealm.cleanup().add(r -> r.users().get(userId).remove());

        assertNotNull(organization.members().member(userId).toRepresentation());
        assertThat(realmRoleNames(userId), hasItem(ROLE_NAME));
        assertThat(organization.invitations().list(), empty());
    }

    private OrganizationResource createOrganizationWithBroker() {
        setUpOrgBroker(providerRealm, consumerRealm, ORG_DOMAIN);

        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(ORG_NAME);
        org.setAlias(ORG_NAME);
        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName(ORG_DOMAIN);
        org.addDomain(domain);

        String orgId;
        try (Response response = consumerRealm.admin().organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            orgId = ApiUtil.getCreatedId(response);
        }

        consumerRealm.cleanup().add(r -> r.organizations().get(orgId).delete().close());
        consumerRealm.admin().organizations().get(orgId).identityProviders().addIdentityProvider(IDP_ALIAS).close();

        return consumerRealm.admin().organizations().get(orgId);
    }

    private String createRealmRole() {
        consumerRealm.admin().roles().create(new RoleRepresentation(ROLE_NAME, "", false));
        consumerRealm.cleanup().add(r -> r.roles().deleteRole(ROLE_NAME));
        return consumerRealm.admin().roles().get(ROLE_NAME).toRepresentation().getId();
    }

    private String getInvitationLink() throws IOException {
        MimeMessage message = mailServer.getLastReceivedMessage();
        assertNotNull(message);
        return MailUtils.getLink(MailUtils.getBody(message).getHtml()).trim();
    }

    private List<String> realmRoleNames(String userId) {
        return consumerRealm.admin().users().get(userId).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .toList();
    }
}
