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

import java.time.Instant;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
@DisplayName("AIA Re-authentication with Brokered Users when organizations are enabled")
public class OrganizationBrokerReAuthTest extends AbstractBrokerReAuthTest {

    private static final String ORG_NAME = "neworg";
    private static final String ORG_DOMAIN = "neworg.org"; // must match USER_EMAIL domain

    // Constants for the cross-org isolation tests
    private static final String ORG_A_NAME = "org-a";
    private static final String ORG_A_DOMAIN = "org-a.org";
    private static final String ORG_B_NAME = "org-b";
    private static final String ORG_B_DOMAIN = "org-b.org";
    private static final String LOCAL_USER_LOGIN = "localuser";
    private static final String LOCAL_USER_EMAIL = "localuser@org-b.org";
    private static final String IDP_A_ALIAS = "idp-a";
    private static final String IDP_B_ALIAS = "idp-b";
    private static final String IDP_A_CLIENT_ID = "idp-a-client";
    private static final String IDP_A_CLIENT_SECRET = "idp-a-secret";
    private static final String IDP_B_CLIENT_ID = "idp-b-client";
    private static final String IDP_B_CLIENT_SECRET = "idp-b-secret";
    private static final String USER_A_LOGIN = "user-a";
    private static final String USER_A_EMAIL = "user-a@org-a.org";
    private static final String USER_B_LOGIN = "user-b";
    private static final String USER_B_EMAIL = "user-b@org-b.org";

    @InjectRealm(ref = CONSUMER_REALM_NAME, config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @Override
    protected ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    protected Instant performFirstBrokerLogin(boolean hideOnLogin) {
        String orgId = createOrg(ORG_NAME, ORG_DOMAIN);
        createOidcIdp(IDP_ALIAS, IDP_CLIENT_ID, IDP_CLIENT_SECRET, ORG_DOMAIN, orgId, hideOnLogin);

        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(USER_EMAIL);
        loginUsernamePage.submit();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM_NAME + "/"),
                "Should be redirected to provider realm for first broker login");
        loginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        loginPage.submit();
        return Instant.now();
    }

    @ParameterizedTest(name = "IdP hidden: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("User in a different org does not see the other org's IdP during re-authentication")
    public void testUserInDifferentOrgDoesNotSeeIdpDuringReAuth(boolean hideIdp) throws InterruptedException {
        String orgAId = createOrg(ORG_A_NAME, ORG_A_DOMAIN);
        String orgBId = createOrg(ORG_B_NAME, ORG_B_DOMAIN);
        createOidcIdp(IDP_ALIAS, IDP_CLIENT_ID, IDP_CLIENT_SECRET, ORG_A_DOMAIN, orgAId, hideIdp);
        String userId = addConsumerUser(LOCAL_USER_LOGIN, LOCAL_USER_EMAIL);
        consumerRealm.admin().organizations().get(orgBId).members().addMember(userId);

        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(LOCAL_USER_EMAIL);
        loginUsernamePage.submit();
        loginPage.fillPassword(USER_PASSWORD);
        Assertions.assertThrows(NoSuchElementException.class, () -> loginPage.findSocialButton(IDP_ALIAS),
                "IdP linked to a different org should not be visible during first authentication");
        loginPage.submit();
        assertTrue(driver.page().getPageSource().contains("Happy days"));
        Instant instantAfterLogin = Instant.now();

        configureUpdateEmailMaxAuthAge();
        waitUntil(instantAfterLogin.plusSeconds(1));
        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).open();

        loginPage.assertCurrent();
        Assertions.assertTrue(driver.page().getPageSource().contains("Please re-authenticate to continue"));
        Assertions.assertThrows(NoSuchElementException.class, () -> loginPage.findSocialButton(IDP_ALIAS),
                "IdP linked to a different org should not be visible during re-authentication");
    }

    @Test
    @DisplayName("During re-authentication each user sees only the IdP linked to their own org")
    public void testEachUserSeesOnlyTheirOrgIdpDuringReAuth() throws InterruptedException {
        String orgAId = createOrg(ORG_A_NAME, ORG_A_DOMAIN);
        String orgBId = createOrg(ORG_B_NAME, ORG_B_DOMAIN);
        createOidcIdp(IDP_A_ALIAS, IDP_A_CLIENT_ID, IDP_A_CLIENT_SECRET, ORG_A_DOMAIN, orgAId);
        createOidcIdp(IDP_B_ALIAS, IDP_B_CLIENT_ID, IDP_B_CLIENT_SECRET, ORG_B_DOMAIN, orgBId);
        addProviderUser(USER_A_LOGIN, USER_A_EMAIL);
        addProviderUser(USER_B_LOGIN, USER_B_EMAIL);
        addProviderClient(IDP_A_CLIENT_ID, IDP_A_CLIENT_SECRET, IDP_A_ALIAS);
        addProviderClient(IDP_B_CLIENT_ID, IDP_B_CLIENT_SECRET, IDP_B_ALIAS);
        configureUpdateEmailMaxAuthAge();

        Instant afterUserALogin = doBrokerLogin(USER_A_EMAIL, USER_A_LOGIN);
        waitUntil(afterUserALogin.plusSeconds(1));
        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).open();
        loginPage.assertCurrent();
        assertTrue(driver.page().getPageSource().contains("Please re-authenticate to continue"));
        Assertions.assertDoesNotThrow(() -> loginPage.findSocialButton(IDP_A_ALIAS));
        Assertions.assertThrows(NoSuchElementException.class, () -> loginPage.findSocialButton(IDP_B_ALIAS),
                "User A should not see org B's IdP during re-authentication");

        logoutUser(USER_A_EMAIL, USER_A_LOGIN);

        Instant afterUserBLogin = doBrokerLogin(USER_B_EMAIL, USER_B_LOGIN);
        waitUntil(afterUserBLogin.plusSeconds(1));
        oauth.loginForm().kcAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).open();
        loginPage.assertCurrent();
        assertTrue(driver.page().getPageSource().contains("Please re-authenticate to continue"));
        Assertions.assertDoesNotThrow(() -> loginPage.findSocialButton(IDP_B_ALIAS));
        Assertions.assertThrows(NoSuchElementException.class, () -> loginPage.findSocialButton(IDP_A_ALIAS),
                "User B should not see org A's IdP during re-authentication");
    }

    private Instant doBrokerLogin(String email, String providerUsername) {
        oauth.openLoginForm();
        loginUsernamePage.assertCurrent();
        loginUsernamePage.fillLoginWithUsernameOnly(email);
        loginUsernamePage.submit();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM_NAME + "/"),
                "Should be redirected to provider realm for first broker login");
        loginPage.fillLogin(providerUsername, USER_PASSWORD);
        loginPage.submit();
        assertTrue(driver.page().getPageSource().contains("Happy days"));
        return Instant.now();
    }

    private void logoutUser(String email, String login) {
        consumerRealm.admin().users().search(email).stream()
                .findFirst()
                .ifPresent(u -> consumerRealm.admin().users().get(u.getId()).logout());
        providerRealm.admin().users().search(login).stream()
                .findFirst()
                .ifPresent(u -> providerRealm.admin().users().get(u.getId()).logout());
        driver.runCleanup();
        driver.cookies().deleteAll();
    }

    private static void waitUntil(Instant instant) throws InterruptedException {
        while (Instant.now().isBefore(instant)) {
            Thread.sleep(100);
        }
    }

    private void configureUpdateEmailMaxAuthAge() {
        RequiredActionProviderRepresentation updateEmailAction = consumerRealm.admin().flows().getRequiredActions()
                .stream()
                .filter(a -> UserModel.RequiredAction.UPDATE_EMAIL.name().equals(a.getProviderId()))
                .findFirst()
                .orElseThrow();
        final boolean originalEnabled = updateEmailAction.isEnabled();
        updateEmailAction.getConfig().put(Constants.MAX_AUTH_AGE_KEY, "0");
        updateEmailAction.setEnabled(true);
        consumerRealm.admin().flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name(), updateEmailAction);
        consumerRealm.cleanup().add(r -> {
            updateEmailAction.getConfig().remove(Constants.MAX_AUTH_AGE_KEY);
            updateEmailAction.setEnabled(originalEnabled);
            r.flows().updateRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name(), updateEmailAction);
        });
    }

    private void createOidcIdp(String alias, String clientId, String clientSecret, String orgDomain, String orgId) {
        createOidcIdp(alias, clientId, clientSecret, orgDomain, orgId, false);
    }

    private void createOidcIdp(String alias, String clientId, String clientSecret, String orgDomain, String orgId, boolean hideOnLogin) {
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create()
                .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                .alias(alias)
                .attribute("clientId", clientId)
                .attribute("clientSecret", clientSecret)
                .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                .attribute("authorizationUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/auth")
                .attribute("tokenUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/token")
                .attribute("jwksUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/certs")
                .attribute("logoutUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/logout")
                .attribute(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, orgDomain)
                .attribute(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString())
                .build();
        idp.setHideOnLogin(hideOnLogin);
        consumerRealm.admin().identityProviders().create(idp).close();
        consumerRealm.cleanup().add(r -> r.identityProviders().get(alias).remove());
        consumerRealm.admin().organizations().get(orgId).identityProviders().addIdentityProvider(alias).close();
    }

    private void addProviderUser(String login, String email) {
        createUser(providerRealm, login, email);
    }

    private String addConsumerUser(String login, String email) {
        return createUser(consumerRealm, login, email);
    }

    private String createUser(ManagedRealm realm, String login, String email) {
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(USER_PASSWORD);
        cred.setTemporary(false);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(login);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setCredentials(List.of(cred));
        String userId;
        try (Response r = realm.admin().users().create(user)) {
            userId = ApiUtil.getCreatedId(r);
        }
        realm.cleanup().add(r -> r.users().get(userId).remove());
        return userId;
    }

    private void addProviderClient(String clientId, String secret, String idpAlias) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setSecret(secret);
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setRedirectUris(List.of(BASE_URL + "/realms/" + CONSUMER_REALM_NAME + "/broker/" + idpAlias + "/endpoint*"));
        String id;
        try (Response r = providerRealm.admin().clients().create(client)) {
            id = ApiUtil.getCreatedId(r);
        }
        providerRealm.cleanup().add(r -> r.clients().get(id).remove());
    }

    private String createOrg(String name, String domain) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);
        org.setAlias(name);
        org.addDomain(new OrganizationDomainRepresentation(domain));
        String orgId;
        try (Response response = consumerRealm.admin().organizations().create(org)) {
            orgId = ApiUtil.getCreatedId(response);
        }
        consumerRealm.cleanup().add(r -> {
            try { r.organizations().get(orgId).delete().close(); } catch (Exception ignored) {}
        });
        return orgId;
    }

    static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
