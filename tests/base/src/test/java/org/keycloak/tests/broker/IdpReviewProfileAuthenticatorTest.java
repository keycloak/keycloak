package org.keycloak.tests.broker;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.HardcodedAttributeMapper;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
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
import org.keycloak.testframework.ui.page.IdpReviewUserProfilePage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

/**
 * Integration tests for {@link org.keycloak.authentication.authenticators.broker.IdpReviewProfileAuthenticator}.
 */
@KeycloakIntegrationTest
public class IdpReviewProfileAuthenticatorTest {

    private static final String IDP_ALIAS = "review-profile-idp";
    private static final String CLIENT_ID = "broker-client";
    private static final String CLIENT_SECRET = "broker-secret";
    private static final String PROVIDER_USERNAME = "provider-user";

    @InjectRealm(ref = "provider", config = ProviderRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(realmRef = "consumer")
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    IdpReviewUserProfilePage reviewProfilePage;

    @BeforeEach
    public void setup() {
        UserProfileResource upResource = consumerRealm.admin().users().userProfile();
        UPConfig upConfig = upResource.getConfiguration();
        UPConfig testUpConfig = upConfig.clone();
        testUpConfig.addOrReplaceAttribute(new UPAttribute("phoneNumber", new UPAttributePermissions(Set.of(ROLE_USER, ROLE_ADMIN), Set.of(ROLE_USER, ROLE_ADMIN))));
        testUpConfig.addOrReplaceAttribute(new UPAttribute("phoneNumberVerified", new UPAttributePermissions(Set.of(ROLE_USER, ROLE_ADMIN), Set.of(ROLE_ADMIN))));
        upResource.update(testUpConfig);
        consumerRealm.cleanup().add(r -> r.users().userProfile().update(upConfig));

        setUpdateProfileOnFirstLogin(IdentityProviderRepresentation.UPFLM_ON);
        addHardcodedAttributeMapper("phone-number-mapper", "phoneNumber", "+15555550123");
        addHardcodedAttributeMapper("phone-number-verified-mapper", "phoneNumberVerified", "true");
        createProviderUser();
    }

    @Test
    public void testReviewProfilePhoneNumberChangeResetsPhoneNumberVerified() {
        loginThroughIdp();

        reviewProfilePage.assertCurrent();
        reviewProfilePage.prepareUpdate().otherProfileAttribute(Map.of("phoneNumber", "+15555550199")).submit();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getConsumerUser();
        Assertions.assertEquals(List.of("+15555550199"), user.getAttributes().get("phoneNumber"));
        Assertions.assertEquals(List.of("false"), user.getAttributes().get("phoneNumberVerified"));
    }

    @Test
    public void testReviewProfilePhoneNumberUnchangedKeepsPhoneNumberVerified() {
        loginThroughIdp();

        reviewProfilePage.assertCurrent();
        reviewProfilePage.prepareUpdate().submit();

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        UserRepresentation user = getConsumerUser();
        Assertions.assertEquals(List.of("+15555550123"), user.getAttributes().get("phoneNumber"));
        Assertions.assertEquals(List.of("true"), user.getAttributes().get("phoneNumberVerified"));
    }

    private void loginThroughIdp() {
        oauth.openLoginForm();
        loginPage.clickSocial(IDP_ALIAS);
        loginPage.fillLogin(PROVIDER_USERNAME, "password");
        loginPage.submit();
    }

    private UserRepresentation getConsumerUser() {
        List<UserRepresentation> users = consumerRealm.admin().users().search(PROVIDER_USERNAME, true);
        Assertions.assertEquals(1, users.size());
        String userId = users.get(0).getId();
        consumerRealm.cleanup().add(r -> r.users().get(userId).remove());
        return consumerRealm.admin().users().get(userId).toRepresentation();
    }

    private void setUpdateProfileOnFirstLogin(String mode) {
        var flows = consumerRealm.admin().flows();
        for (AuthenticationExecutionInfoRepresentation execution : flows.getExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW)) {
            if (DefaultAuthenticationFlows.IDP_REVIEW_PROFILE_CONFIG_ALIAS.equals(execution.getAlias())) {
                AuthenticatorConfigRepresentation config = flows.getAuthenticatorConfig(execution.getAuthenticationConfig());
                String previousMode = config.getConfig().get("update.profile.on.first.login");
                config.getConfig().put("update.profile.on.first.login", mode);
                flows.updateAuthenticatorConfig(config.getId(), config);
                consumerRealm.cleanup().add(r -> {
                    config.getConfig().put("update.profile.on.first.login", previousMode);
                    r.flows().updateAuthenticatorConfig(config.getId(), config);
                });
            }
        }
    }

    private void addHardcodedAttributeMapper(String mapperName, String attribute, String value) {
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setName(mapperName);
        mapper.setIdentityProviderAlias(IDP_ALIAS);
        mapper.setIdentityProviderMapper(HardcodedAttributeMapper.PROVIDER_ID);
        mapper.setConfig(Map.of(
                HardcodedAttributeMapper.ATTRIBUTE, attribute,
                HardcodedAttributeMapper.ATTRIBUTE_VALUE, value
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
