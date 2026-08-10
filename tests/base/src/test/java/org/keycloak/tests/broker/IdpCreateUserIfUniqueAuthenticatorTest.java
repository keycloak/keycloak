package org.keycloak.tests.broker;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.UsernameTemplateMapper;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.validation.Validation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticator}.
 */
@KeycloakIntegrationTest
public class IdpCreateUserIfUniqueAuthenticatorTest {

    private static final String IDP_ALIAS = "create-user-idp";
    private static final String CLIENT_ID = "broker-client";
    private static final String CLIENT_SECRET = "broker-secret";
    private static final String PROVIDER_USERNAME = "provider-user";

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @Test // #50903
    public void testIdpBrokerLoginRejectsUsernameLongerThanMaxLength() {
        // Use a mapper to inject username that is too long, don't persist length in the db.
        String oversizedUsername = "a".repeat(Validation.MAX_USERNAME_LENGTH + 1);
        addUsernameTemplateMapper("oversized-username-mapper", oversizedUsername);

        createProviderUser();
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);

        assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"),
                "Expected redirect to provider realm login page");

        loginPage.fillLogin(PROVIDER_USERNAME, "password");
        loginPage.submit();
        assertTrue("login-idp-review-user-profile".equals(driver.page().getCurrentPageId()),
                "Expected broker flow to restart at IDP review step after resetFlow(), but page was: "
                        + driver.page().getCurrentPageId());
        consumerRealm.admin().users().search(oversizedUsername).forEach(u ->
                consumerRealm.cleanup().add(r -> r.users().get(u.getId()).remove()));
        List<UserRepresentation> created = consumerRealm.admin().users().search(oversizedUsername);
        assertTrue(created.isEmpty(), "No user with an oversized username should be created in the consumer realm");
    }

    @Test // #50903
    public void testIdpBrokerLoginAcceptsUsernameAtMaxLength() {
        String boundaryUsername = "a".repeat(Validation.MAX_USERNAME_LENGTH);
        addUsernameTemplateMapper("boundary-username-mapper", boundaryUsername);

        createProviderUser();
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);

        assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"),
                "Expected redirect to provider realm login page");

        loginPage.fillLogin(PROVIDER_USERNAME, "password");
        loginPage.submit();

        assertFalse("login-idp-review-user-profile".equals(driver.page().getCurrentPageId()),
                "Boundary-length username must not trigger the length guard resetFlow()");

        List<UserRepresentation> created = consumerRealm.admin().users().search(boundaryUsername, true);
        assertFalse(created.isEmpty(),
                "A user with a boundary-length username must be created in the consumer realm");
        created.forEach(u -> consumerRealm.cleanup().add(r -> r.users().get(u.getId()).remove()));
    }

    private void addUsernameTemplateMapper(String mapperName, String usernameTemplate) {
        // adds to consumer IDP, replaces brokered username with mapperName
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName(mapperName);
        mapper.setIdentityProviderAlias(IDP_ALIAS);
        mapper.setIdentityProviderMapper(UsernameTemplateMapper.PROVIDER_ID);
        mapper.setConfig(Map.of(
                UsernameTemplateMapper.TEMPLATE, usernameTemplate,
                "target", "LOCAL"
        ));
        try (Response response = consumerRealm.admin().identityProviders().get(IDP_ALIAS).addMapper(mapper)) {
            Assertions.assertEquals(201, response.getStatus());
            String mapperId = ApiUtil.getCreatedId(response);
            consumerRealm.cleanup().add(r -> r.identityProviders().get(IDP_ALIAS).delete(mapperId));
        }
    }

    private void createProviderUser() {
        UserRepresentation user = UserBuilder.create(PROVIDER_USERNAME)
                .password("password")
                .email(PROVIDER_USERNAME + "@example.com")
                .emailVerified(true)
                .firstName("Provider")
                .lastName("User")
                .build();
        try (Response response = providerRealm.admin().users().create(user)) {
            Assertions.assertEquals(201, response.getStatus());
            String userId = ApiUtil.getCreatedId(response);
            providerRealm.cleanup().add(r -> r.users().get(userId).remove());
        }
    }

    public static class ProviderRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(
                    ClientBuilder.create(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("*")
                            .directAccessGrantsEnabled()
            );
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            String providerBase = "http://localhost:8080/realms/provider";

            IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
            idp.setAlias(IDP_ALIAS);
            idp.setProviderId(OIDCIdentityProviderFactory.PROVIDER_ID);
            idp.setEnabled(true);
            idp.setTrustEmail(true);
            idp.setConfig(Map.of(
                    "clientId",          CLIENT_ID,
                    "clientSecret",      CLIENT_SECRET,
                    "authorizationUrl",  providerBase + "/protocol/openid-connect/auth",
                    "tokenUrl",          providerBase + "/protocol/openid-connect/token",
                    "userInfoUrl",       providerBase + "/protocol/openid-connect/userinfo",
                    "jwksUrl",           providerBase + "/protocol/openid-connect/certs",
                    "defaultScope",      "openid email profile",
                    IdentityProviderModel.SYNC_MODE, "IMPORT"
            ));
            return realm.identityProviders(idp);
        }
    }
}
