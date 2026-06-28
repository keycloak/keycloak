package org.keycloak.tests.organization.admin;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.representations.account.LinkedAccountRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.CredentialBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationMemberIdpLinkTest {

    private static final String IDP_ALIAS = "org-identity-provider";
    private static final String SECOND_IDP_ALIAS = "second-identity-provider";
    private static final String CLIENT_ID = "broker-app";
    private static final String CLIENT_SECRET = "broker-secret";
    private static final String ORG_NAME = "testorg";
    private static final String ORG_DOMAIN = "testorg.org";

    @InjectRealm(ref = "provider", config = ProviderRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = ConsumerRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectUser(ref = "alice", realmRef = "provider", config = AliceUserConf.class)
    ManagedUser aliceFromProviderRealm;

    @InjectOAuthClient(ref = "consumer", realmRef = "consumer")
    OAuthClient consumerOAuth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @Test
    public void testAdminCannotAddFederatedIdentityToManagedMember() {
        OrganizationRepresentation org = createOrganization();
        String managedUserId = loginViaBrokerAndGetUserId();

        verifyManagedMember(org.getId(), managedUserId);

        addSecondIdp();

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setUserId("external-user-id");
        link.setUserName("external-username");

        try (Response response = consumerRealm.admin().users().get(managedUserId).addFederatedIdentity(SECOND_IDP_ALIAS, link)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testAccountLinkedAccountsReturnsEmptyForManagedMember() throws Exception {
        OrganizationRepresentation org = createOrganization();
        String managedUserId = loginViaBrokerAndGetUserId();

        verifyManagedMember(org.getId(), managedUserId);

        consumerRealm.admin().users().get(managedUserId)
                .resetPassword(CredentialBuilder.password("password").build());

        addSecondIdp();

        AccessTokenResponse tokenResponse = consumerOAuth.doPasswordGrantRequest(
                aliceFromProviderRealm.getUsername(), "password");
        assertNotNull(tokenResponse.getAccessToken());

        String accountUrl = consumerRealm.getBaseUrl() + "/account/linked-accounts?linked=false";
        List<LinkedAccountRepresentation> available = simpleHttp.doGet(accountUrl)
                .auth(tokenResponse.getAccessToken())
                .acceptJson()
                .asJson(new TypeReference<>() {});

        assertTrue(available.isEmpty(),
                "Managed organization member should not see any providers available for linking");
    }

    @Test
    public void testUnmanagedMemberCanLinkAdditionalIdp() {
        OrganizationRepresentation org = createOrganization();
        OrganizationResource orgResource = consumerRealm.admin().organizations().get(org.getId());

        UserRepresentation unmanagedUser = new UserRepresentation();
        unmanagedUser.setUsername("unmanaged@testorg.org");
        unmanagedUser.setEmail("unmanaged@testorg.org");
        unmanagedUser.setEnabled(true);
        String userId;
        try (Response response = consumerRealm.admin().users().create(unmanagedUser)) {
            userId = ApiUtil.getCreatedId(response);
        }
        String finalUserId = userId;
        consumerRealm.cleanup().add(r -> r.users().get(finalUserId).remove());

        try (Response response = orgResource.members().addMember(userId)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        addSecondIdp();

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setUserId("external-user-id");
        link.setUserName("external-username");

        try (Response response = consumerRealm.admin().users().get(userId).addFederatedIdentity(SECOND_IDP_ALIAS, link)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // cleanup
        consumerRealm.admin().users().get(userId).removeFederatedIdentity(SECOND_IDP_ALIAS);
    }

    private OrganizationRepresentation createOrganization() {
        setUpOrgBroker();

        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(ORG_NAME);
        org.setAlias(ORG_NAME);
        org.addDomain(new org.keycloak.representations.idm.OrganizationDomainRepresentation());
        org.getDomains().iterator().next().setName(ORG_DOMAIN);

        String orgId;
        try (Response response = consumerRealm.admin().organizations().create(org)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            orgId = ApiUtil.getCreatedId(response);
        }

        String finalOrgId = orgId;
        consumerRealm.cleanup().add(r -> {
            try {
                r.organizations().get(finalOrgId).delete().close();
            } catch (Exception ignored) {}
        });

        consumerRealm.admin().organizations().get(orgId)
                .identityProviders().addIdentityProvider(IDP_ALIAS).close();

        return consumerRealm.admin().organizations().get(orgId).toRepresentation();
    }

    private String loginViaBrokerAndGetUserId() {
        consumerOAuth.openLoginForm();

        loginUsernamePage.fillLoginWithUsernameOnly("alice@" + ORG_DOMAIN);
        loginUsernamePage.submit();

        assertTrue(driver.getCurrentUrl().contains("/realms/" + providerRealm.getName() + "/"),
                "Should be on provider realm login page");

        loginPage.fillLogin(aliceFromProviderRealm.getUsername(), aliceFromProviderRealm.getPassword());
        loginPage.submit();

        List<UserRepresentation> users = consumerRealm.admin().users()
                .search(aliceFromProviderRealm.getUsername());
        assertEquals(1, users.size(), "Federated user should be created in consumer realm");

        String userId = users.get(0).getId();
        consumerRealm.cleanup().add(r -> {
            try {
                r.users().get(userId).remove();
            } catch (jakarta.ws.rs.NotFoundException ignored) {}
        });
        return userId;
    }

    private void verifyManagedMember(String orgId, String userId) {
        MemberRepresentation member = consumerRealm.admin().organizations().get(orgId)
                .members().member(userId).toRepresentation();
        assertNotNull(member);
        assertEquals(org.keycloak.representations.idm.MembershipType.MANAGED, member.getMembershipType());
    }

    private void addSecondIdp() {
        try {
            consumerRealm.admin().identityProviders().get(SECOND_IDP_ALIAS).toRepresentation();
        } catch (Exception e) {
            IdentityProviderRepresentation secondIdp = new IdentityProviderRepresentation();
            secondIdp.setAlias(SECOND_IDP_ALIAS);
            secondIdp.setProviderId("oidc");
            secondIdp.setEnabled(true);
            secondIdp.setConfig(Map.of("clientId", "dummy", "clientSecret", "dummy",
                    "authorizationUrl", "http://localhost/dummy/auth",
                    "tokenUrl", "http://localhost/dummy/token"));
            consumerRealm.admin().identityProviders().create(secondIdp).close();
            consumerRealm.cleanup().add(r -> {
                try {
                    r.identityProviders().get(SECOND_IDP_ALIAS).remove();
                } catch (Exception ignored) {}
            });
        }
    }

    static class AliceUserConf implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder builder) {
            return builder.username("alice")
                    .password("password")
                    .email("alice@testorg.org")
                    .emailVerified(true)
                    .name("Alice", "Org");
        }
    }

    static class ProviderRealmConf implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(
                    ClientBuilder.create(CLIENT_ID)
                            .name(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("*")
                            .directAccessGrantsEnabled()
            );
        }
    }

    static class ConsumerRealmConf implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }

    private void setUpOrgBroker() {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(IDP_ALIAS);
        idp.setProviderId("keycloak-oidc");
        idp.setEnabled(true);
        idp.setStoreToken(false);
        idp.setTrustEmail(true);

        String providerBaseUrl = providerRealm.getBaseUrl();
        Map<String, String> config = Map.of(
                "clientId", CLIENT_ID,
                "clientSecret", CLIENT_SECRET,
                "authorizationUrl", providerBaseUrl + "/protocol/openid-connect/auth",
                "tokenUrl", providerBaseUrl + "/protocol/openid-connect/token",
                "userInfoUrl", providerBaseUrl + "/protocol/openid-connect/userinfo",
                "defaultScope", "email profile",
                "syncMode", "IMPORT",
                OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, ORG_DOMAIN,
                IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString()
        );
        idp.setConfig(config);

        consumerRealm.admin().identityProviders().create(idp).close();
        consumerRealm.cleanup().add(r -> {
            try {
                r.identityProviders().get(IDP_ALIAS).remove();
            } catch (Exception ignored) {}
        });
    }
}
