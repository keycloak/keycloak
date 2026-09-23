package org.keycloak.tests.broker;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies the runtime (broker-login) enforcement of the identity provider {@code allowAdminRoleMapping}
 * switch, implemented in {@link org.keycloak.broker.provider.AbstractIdentityProviderMapper#isAdminRoleGrantAllowed}.
 * <p>
 * The admin REST tests in {@code RealmAdminAccessTest} only cover the creation-time guard (a mapper granting
 * an admin role is rejected with 403 while the switch is off). This test covers the second, independent
 * layer: even when such a mapper already exists on the identity provider, an actual broker login must not
 * grant the admin role while the switch is disabled, and must grant it once the switch is enabled.
 * <p>
 * Because the creation-time guard blocks creating an admin-role mapper while the switch is off, the mapper is
 * added while the switch is on and the switch is then toggled - mirroring the real defense-in-depth scenario
 * where a mapper created under an enabled switch must stop taking effect once an administrator disables it.
 */
@KeycloakIntegrationTest
public class IdentityProviderMapperAdminRoleBrokerLoginTest {

    private static final String IDP_ALIAS = "kc-oidc-idp";
    private static final String CLIENT_ID = "broker-client";
    private static final String CLIENT_SECRET = "broker-secret";
    private static final String USER_LOGIN = "testuser";
    private static final String USER_PASSWORD = "password";

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @Test
    public void adminRoleNotGrantedDuringLoginWhenAllowAdminRoleMappingDisabled() {
        // The mapper can only be created while the switch is on (creation-time guard); disable it afterwards
        // so the login-time check is the only thing standing between the mapper and the admin-role grant.
        setAllowAdminRoleMapping(true);
        addHardcodedManageRealmRoleMapper();
        setAllowAdminRoleMapping(false);

        loginThroughBroker();

        Assertions.assertFalse(consumerUserHasManageRealmRole(),
                "The runtime check must block the admin-role grant during broker login when allowAdminRoleMapping is disabled");
    }

    @Test
    public void adminRoleGrantedDuringLoginWhenAllowAdminRoleMappingEnabled() {
        setAllowAdminRoleMapping(true);
        addHardcodedManageRealmRoleMapper();

        loginThroughBroker();

        Assertions.assertTrue(consumerUserHasManageRealmRole(),
                "The admin-role mapper must grant the admin role during broker login when allowAdminRoleMapping is enabled");
    }

    private void setAllowAdminRoleMapping(boolean allow) {
        IdentityProviderRepresentation idp = consumerRealm.admin().identityProviders().get(IDP_ALIAS).toRepresentation();
        idp.getConfig().put(IdentityProviderModel.ALLOW_ADMIN_ROLE_MAPPING, String.valueOf(allow));
        consumerRealm.admin().identityProviders().get(IDP_ALIAS).update(idp);
    }

    private void addHardcodedManageRealmRoleMapper() {
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName("grant-realm-admin");
        mapper.setIdentityProviderAlias(IDP_ALIAS);
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        mapper.setConfig(Map.of(
                "role", Constants.REALM_MANAGEMENT_CLIENT_ID + "." + AdminRoles.MANAGE_REALM,
                "syncMode", "INHERIT"));
        try (Response response = consumerRealm.admin().identityProviders().get(IDP_ALIAS).addMapper(mapper)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(),
                    "Adding the admin-role mapper must succeed while allowAdminRoleMapping is enabled");
        }
    }

    private void loginThroughBroker() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        // Now on the provider realm's login page (same page object, different realm).
        loginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        loginPage.submit();
    }

    private boolean consumerUserHasManageRealmRole() {
        List<UserRepresentation> users = consumerRealm.admin().users().search(USER_LOGIN, true);
        Assertions.assertEquals(1, users.size(), "Brokered user should exist in the consumer realm after login");
        String userId = users.get(0).getId();
        ClientRepresentation realmManagement = consumerRealm.admin().clients()
                .findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        return consumerRealm.admin().users().get(userId).roles()
                .clientLevel(realmManagement.getId()).listAll().stream()
                .anyMatch(role -> AdminRoles.MANAGE_REALM.equals(role.getName()));
    }

    public static class ProviderRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .clients(ClientBuilder.create(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("*"))
                    // The provider user is complete (first/last name, verified email) so the consumer's
                    // first-broker-login review-profile page does not interrupt the redirect back.
                    .users(UserBuilder.create(USER_LOGIN)
                            .password(USER_PASSWORD)
                            .email(USER_LOGIN + "@example.com")
                            .emailVerified(true)
                            .firstName("Test")
                            .lastName("User")
                            .enabled(true));
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
                    "clientId", CLIENT_ID,
                    "clientSecret", CLIENT_SECRET,
                    "authorizationUrl", providerBase + "/protocol/openid-connect/auth",
                    "tokenUrl", providerBase + "/protocol/openid-connect/token",
                    "userInfoUrl", providerBase + "/protocol/openid-connect/userinfo",
                    "jwksUrl", providerBase + "/protocol/openid-connect/certs",
                    "defaultScope", "openid email profile",
                    IdentityProviderModel.SYNC_MODE, "IMPORT"));
            return realm.identityProviders(idp);
        }
    }
}
