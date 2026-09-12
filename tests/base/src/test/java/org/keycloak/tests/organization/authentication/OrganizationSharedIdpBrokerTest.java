package org.keycloak.tests.organization.authentication;

import java.util.List;
import java.util.Objects;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OrganizationSharedIdpBrokerTest {

    private static final String SHARED_IDP_ALIAS = "shared-identity-provider";
    private static final String ORG_A_NAME = "org-a";
    private static final String ORG_A_DOMAIN = "orga.org";
    private static final String ORG_B_NAME = "org-b";
    private static final String ORG_B_DOMAIN = "orgb.org";

    @InjectRealm(ref = "provider", config = AbstractOrganizationTest.ProviderRealmConf.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", config = AbstractOrganizationTest.OrganizationRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectOAuthClient(ref = "consumer", realmRef = "consumer")
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectPage
    LoginUpdateProfilePage loginUpdateProfilePage;

    @Test
    @SuppressWarnings("unchecked")
    public void testSharedIdpResolvesOrgFromEmailDomain() {
        createSharedIdpSetup();
        createProviderUser("alice", "alice@" + ORG_A_DOMAIN, "password");
        createProviderUser("bob", "bob@" + ORG_B_DOMAIN, "password");

        oauth.scope("organization:*");
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly("alice@" + ORG_A_DOMAIN);
        loginUsernamePage.submit();

        assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains("/realms/" + providerRealm.getName() + "/"),
                "Should be redirected to provider realm");

        loginPage.fillLogin("alice", "password");
        loginPage.submit();

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull(code, "Should have received an auth code for alice");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        List<String> orgs = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertNotNull(orgs, "Token should contain organization claim");
        assertThat("alice@orga.org should resolve to org-a", orgs, hasItem(ORG_A_NAME));
        assertThat("alice@orga.org should not resolve to org-b", orgs, not(hasItem(ORG_B_NAME)));

        String aliceId = consumerRealm.admin().users().searchByEmail("alice@" + ORG_A_DOMAIN, true).get(0).getId();
        consumerRealm.admin().users().get(aliceId).logout();
        consumerRealm.cleanup().add(r -> {
            try { r.users().get(aliceId).remove(); } catch (Exception ignored) {}
        });
        List<UserRepresentation> providerAliceList = providerRealm.admin().users().search("alice");
        if (!providerAliceList.isEmpty()) {
            providerRealm.admin().users().get(providerAliceList.get(0).getId()).logout();
        }

        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly("bob@" + ORG_B_DOMAIN);
        loginUsernamePage.submit();

        assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains("/realms/" + providerRealm.getName() + "/"),
                "Should be redirected to provider realm");

        loginPage.fillLogin("bob", "password");
        loginPage.submit();

        code = oauth.parseLoginResponse().getCode();
        assertNotNull(code, "Should have received an auth code for bob");
        tokenResponse = oauth.doAccessTokenRequest(code);
        accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        orgs = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertNotNull(orgs, "Token should contain organization claim");
        assertThat("bob@orgb.org should resolve to org-b", orgs, hasItem(ORG_B_NAME));
        assertThat("bob@orgb.org should not resolve to org-a", orgs, not(hasItem(ORG_A_NAME)));

        String bobId = consumerRealm.admin().users().searchByEmail("bob@" + ORG_B_DOMAIN, true).get(0).getId();
        consumerRealm.cleanup().add(r -> {
            try { r.users().get(bobId).remove(); } catch (Exception ignored) {}
        });
    }

    @Test
    public void testAuthNotePreservedOverridesEmailDomain() {
        createSharedIdpSetup();
        createProviderUser("alice", "alice@" + ORG_A_DOMAIN, "password");

        oauth.scope("organization:*");
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly("newuser@" + ORG_B_DOMAIN);
        loginUsernamePage.submit();

        assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains("/realms/" + providerRealm.getName() + "/"),
                "Should be redirected to provider realm via org-B's domain routing");

        loginPage.fillLogin("alice", "password");
        loginPage.submit();

        loginUpdateProfilePage.update("New", "User", "alice@" + ORG_A_DOMAIN);
        assertTrue(driver.driver().getPageSource().contains("Email domain does not match any domain from the organization"),
                "Email domain validation should reject orga.org when org context is org-B");

        loginUpdateProfilePage.update("New", "User", "newuser@" + ORG_B_DOMAIN);

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull(code, "Should have received an auth code after profile update");

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        @SuppressWarnings("unchecked")
        List<String> orgs = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertNotNull(orgs, "Token should contain organization claim");
        assertThat("Org context should be org-B from auth note, not org-A from email domain",
                orgs, hasItem(ORG_B_NAME));

        String userId = consumerRealm.admin().users().searchByEmail("newuser@" + ORG_B_DOMAIN, true).get(0).getId();
        consumerRealm.cleanup().add(r -> {
            try { r.users().get(userId).remove(); } catch (Exception ignored) {}
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSingleOrgIdpFallback() {
        String singleIdpAlias = "single-org-idp";
        IdentityProviderRepresentation idp = AbstractOrganizationTest.createRealOrgBroker(singleIdpAlias, providerRealm);
        consumerRealm.admin().identityProviders().create(idp).close();
        consumerRealm.cleanup().add(r -> {
            try { r.identityProviders().get(singleIdpAlias).remove(); } catch (Exception ignored) {}
        });

        String orgId = createOrganization(ORG_A_NAME, ORG_A_DOMAIN);
        consumerRealm.admin().organizations().get(orgId).identityProviders().addIdentityProvider(singleIdpAlias).close();
        setDomainRouting(orgId, ORG_A_DOMAIN, singleIdpAlias);

        createProviderUser("alice", "alice@" + ORG_A_DOMAIN, "password");

        oauth.scope("organization:*");
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly("alice@" + ORG_A_DOMAIN);
        loginUsernamePage.submit();

        assertTrue(Objects.requireNonNull(driver.getCurrentUrl()).contains("/realms/" + providerRealm.getName() + "/"),
                "Should be redirected to provider realm");

        loginPage.fillLogin("alice", "password");
        loginPage.submit();

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull(code, "Should have received an auth code");
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        List<String> orgs = (List<String>) accessToken.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertNotNull(orgs, "Token should contain organization claim");
        assertThat(orgs, hasItem(ORG_A_NAME));

        String userId = consumerRealm.admin().users().searchByEmail("alice@" + ORG_A_DOMAIN, true).get(0).getId();
        consumerRealm.cleanup().add(r -> {
            try { r.users().get(userId).remove(); } catch (Exception ignored) {}
        });
    }

    private void createSharedIdpSetup() {
        IdentityProviderRepresentation idp = AbstractOrganizationTest.createRealOrgBroker(SHARED_IDP_ALIAS, providerRealm);
        consumerRealm.admin().identityProviders().create(idp).close();
        consumerRealm.cleanup().add(r -> {
            try { r.identityProviders().get(SHARED_IDP_ALIAS).remove(); } catch (Exception ignored) {}
        });

        String orgAId = createOrganization(ORG_A_NAME, ORG_A_DOMAIN);
        String orgBId = createOrganization(ORG_B_NAME, ORG_B_DOMAIN);

        consumerRealm.admin().organizations().get(orgAId).identityProviders().addIdentityProvider(SHARED_IDP_ALIAS).close();
        consumerRealm.admin().organizations().get(orgBId).identityProviders().addIdentityProvider(SHARED_IDP_ALIAS).close();

        setDomainRouting(orgAId, ORG_A_DOMAIN, SHARED_IDP_ALIAS);
        setDomainRouting(orgBId, ORG_B_DOMAIN, SHARED_IDP_ALIAS);
    }

    private String createOrganization(String name, String domain) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);
        org.setAlias(name);
        OrganizationDomainRepresentation domainRep = new OrganizationDomainRepresentation();
        domainRep.setName(domain);
        org.addDomain(domainRep);

        String orgId;
        try (Response response = consumerRealm.admin().organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            orgId = ApiUtil.getCreatedId(response);
        }

        String finalOrgId = orgId;
        consumerRealm.cleanup().add(r -> {
            try { r.organizations().get(finalOrgId).delete().close(); } catch (Exception ignored) {}
        });

        return orgId;
    }

    private void setDomainRouting(String orgId, String domain, String idpAlias) {
        OrganizationRepresentation org = consumerRealm.admin().organizations().get(orgId).toRepresentation();
        org.getDomains().stream()
                .filter(d -> d.getName().equals(domain))
                .findFirst()
                .ifPresent(d -> {
                    d.setIdentityProviderAlias(idpAlias);
                    d.setAutoRedirect(true);
                });
        consumerRealm.admin().organizations().get(orgId).update(org).close();
    }

    private void createProviderUser(String username, String email, String password) {
        UserRepresentation user = UserBuilder.create()
                .username(username)
                .password(password)
                .email(email)
                .emailVerified(true)
                .name(username.substring(0, 1).toUpperCase() + username.substring(1), "User")
                .enabled(true)
                .build();
        String userId;
        try (Response response = providerRealm.admin().users().create(user)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            userId = ApiUtil.getCreatedId(response);
        }
        String finalUserId = userId;
        providerRealm.cleanup().add(r -> {
            try { r.users().get(finalUserId).remove(); } catch (Exception ignored) {}
        });
    }
}
