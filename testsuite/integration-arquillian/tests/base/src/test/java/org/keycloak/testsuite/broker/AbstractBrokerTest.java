package org.keycloak.testsuite.broker;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;

public abstract class AbstractBrokerTest extends AbstractKeycloakTest {

    protected abstract RealmRepresentation createProviderRealm();
    protected abstract RealmRepresentation createConsumerRealm();

    protected abstract List<ClientRepresentation> createProviderClients();
    protected abstract List<ClientRepresentation> createConsumerClients();

    protected abstract IdentityProviderRepresentation setUpIdentityProvider();

    protected abstract String providerRealmName();
    protected abstract String consumerRealmName();

    protected abstract String getUserLogin();
    protected abstract String getUserPassword();
    protected abstract String getUserEmail();

    protected abstract String getIDPAlias();

    @Page
    protected LoginPage accountLoginPage;
    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation providerRealm = createProviderRealm();
        RealmRepresentation consumerRealm = createConsumerRealm();

        testRealms.add(providerRealm);
        testRealms.add(consumerRealm);
    }

    @Before
    public void createUser() {
        log.debug("creating user for realm " + providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(getUserLogin());
        user.setEmail(getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(providerRealmName());
        String userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), getUserPassword(), false);
    }

    @Before
    public void addIdentityProviderToProviderRealm() {
        log.debug("adding identity provider to realm " + consumerRealmName());

        RealmResource realm = adminClient.realm(consumerRealmName());
        realm.identityProviders().create(setUpIdentityProvider());
    }

    @Before
    public void addClients() {
        List<ClientRepresentation> clients = createProviderClients();
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + providerRealmName());

                providerRealm.clients().create(client);
            }
        }

        clients = createConsumerClients();
        if (clients != null) {
            RealmResource consumerRealm = adminClient.realm(consumerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + consumerRealmName());

                consumerRealm.clients().create(client);
            }
        }
    }

    protected String getAuthRoot() {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    protected IdentityProviderRepresentation createIdentityProvider(String alias, String providerId) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(alias);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

    private void waitForPage(String title) {
        long startAt = System.currentTimeMillis();

        while (!driver.getTitle().toLowerCase().contains(title)
                && System.currentTimeMillis() - startAt < 200) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignore) {}
        }
    }

    @Test
    public void logInAsUserInIDP() {
        driver.navigate().to(getAccountUrl(consumerRealmName()));

        log.debug("Clicking social " + getIDPAlias());
        accountLoginPage.clickSocial(getIDPAlias());

        if (!driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/")) {
            log.debug("Not on provider realm page, url: " + driver.getCurrentUrl());
        }

        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName() + "/"));

        log.debug("Logging in");
        accountLoginPage.login(getUserLogin(), getUserPassword());

        waitForPage("update account information");

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(consumerRealmName()).users();
        Assert.assertTrue("There must be at least one user", consumerUsers.count() > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, 5);

        boolean isUserFound = false;
        for (UserRepresentation user : users) {
            if (user.getUsername().equals(getUserLogin()) && user.getEmail().equals(getUserEmail())) {
                isUserFound = true;
                break;
            }
        }

        Assert.assertTrue("There must be user " + getUserLogin() + " in realm " + consumerRealmName(),
                isUserFound);

        testSingleLogout();
    }

    protected void testSingleLogout() {
        log.debug("Testing single log out");

        driver.navigate().to(getAccountUrl(providerRealmName()));

        Assert.assertTrue("Should be logged in the account page", driver.getTitle().endsWith("Account Management"));

        String encodedAccount;
        try {
            encodedAccount = URLEncoder.encode(getAccountUrl(providerRealmName()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedAccount = getAccountUrl(providerRealmName());
        }

        driver.navigate().to(getAuthRoot()
                + "/auth/realms/" + providerRealmName()
                + "/protocol/" + "openid-connect"
                + "/logout?redirect_uri=" + encodedAccount);

        waitForPage("log in to " + providerRealmName());

        Assert.assertTrue("Should be on " + providerRealmName() + " realm", driver.getCurrentUrl().contains("/auth/realms/" + providerRealmName()));

        driver.navigate().to(getAccountUrl(consumerRealmName()));

        Assert.assertTrue("Should be on " + consumerRealmName() + " realm on login page",
                driver.getCurrentUrl().contains("/auth/realms/" + consumerRealmName() + "/protocol/openid-connect/"));
    }

    private String getAccountUrl(String realmName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/account";
    }
}
