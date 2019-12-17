/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.social.google.GoogleIdentityProviderFactory;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.LinkedAccountsPage;
import org.keycloak.testsuite.util.ClientBuilder;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class LinkedAccountsTest extends BaseAccountPageTest {
    public static final String SOCIAL_IDP_ALIAS = "fake-google-account";
    public static final String SYSTEM_IDP_ALIAS = "kc-to-kc-account";

    public static final String REALM2_NAME = "test-realm2";
    public static final String CLIENT_ID = "cross-realm-client";
    public static final String CLIENT_SECRET = "top secret";

    private UserRepresentation homerUser;

    private LinkedAccountsPage.IdentityProvider socialIdp;
    private LinkedAccountsPage.IdentityProvider systemIdp;

    @Page
    private LinkedAccountsPage linkedAccountsPage;

    @Page
    private LoginPage loginPageWithSocialBtns;

    public LinkedAccountsTest() {
        // needs to be done here (setting fields in addTestRealms acts really weird resulting in Homer being null)
        homerUser = createUserRepresentation("hsimpson", "hsimpson@keycloak.org",
                "Homer", "Simpson", true, "Mmm donuts");
    }

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return linkedAccountsPage;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation realm1 = testRealms.get(0);

        realm1.addIdentityProvider(createIdentityProviderRepresentation(SOCIAL_IDP_ALIAS,
                GoogleIdentityProviderFactory.PROVIDER_ID));

        String oidcRoot = getAuthServerRoot() + "realms/" + REALM2_NAME + "/protocol/openid-connect/";

        IdentityProviderRepresentation systemIdp = createIdentityProviderRepresentation(SYSTEM_IDP_ALIAS,
                OIDCIdentityProviderFactory.PROVIDER_ID);
        systemIdp.getConfig().put("clientId", CLIENT_ID);
        systemIdp.getConfig().put("clientSecret", CLIENT_SECRET);
        systemIdp.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);
        systemIdp.getConfig().put("authorizationUrl", oidcRoot + "auth");
        systemIdp.getConfig().put("tokenUrl", oidcRoot + "token");
        realm1.addIdentityProvider(systemIdp);

        ClientRepresentation client = ClientBuilder.create()
                .clientId(CLIENT_ID)
                .secret(CLIENT_SECRET)
                .redirectUris(getAuthServerRoot() + "realms/" + TEST + "/broker/" + SYSTEM_IDP_ALIAS + "/endpoint")
                .build();

        // using REALM2 as an identity provider
        RealmRepresentation realm2 = new RealmRepresentation();
        realm2.setId(REALM2_NAME);
        realm2.setRealm(REALM2_NAME);
        realm2.setEnabled(true);
        realm2.setClients(Collections.singletonList(client));
        realm2.setUsers(Collections.singletonList(homerUser));
        testRealms.add(realm2);
    }

    @Before
    public void beforeLinkedAccountsTest() {
        socialIdp = linkedAccountsPage.getProvider(SOCIAL_IDP_ALIAS);
        systemIdp = linkedAccountsPage.getProvider(SYSTEM_IDP_ALIAS);
        assertProvidersCount();
    }

    @After
    public void afterLinkedAccountsTest() {
        assertProvidersCount();
    }

    @Test
    public void linkAccountTest() {
        assertEquals(0, testUserResource().getFederatedIdentity().size());

        assertProvider(socialIdp, false, true, "");
        assertProvider(systemIdp, false, false, "");

        systemIdp.clickLinkBtn();
        loginPage.form().login(homerUser);
        linkedAccountsPage.assertCurrent();
        assertProvider(systemIdp, true, false, homerUser.getUsername());

        assertProvider(socialIdp, false, true, "");

        // check through admin REST endpoints
        List<FederatedIdentityRepresentation> fids = testUserResource().getFederatedIdentity();
        assertEquals(1, fids.size());
        FederatedIdentityRepresentation fid = fids.get(0);
        assertEquals(SYSTEM_IDP_ALIAS, fid.getIdentityProvider());
        assertEquals(homerUser.getUsername(), fid.getUserName());

        // try to login using IdP
        deleteAllSessionsInTestRealm();
        linkedAccountsPage.navigateTo();
        loginPageWithSocialBtns.clickSocial(SYSTEM_IDP_ALIAS);
        linkedAccountsPage.assertCurrent(); // no need for re-login to REALM2
    }

    @Test
    public void unlinkAccountTest() {
        FederatedIdentityRepresentation fid = new FederatedIdentityRepresentation();
        fid.setIdentityProvider(SOCIAL_IDP_ALIAS);
        fid.setUserId("Homer lost his ID at Moe's last night");
        fid.setUserName(homerUser.getUsername());
        testUserResource().addFederatedIdentity(SOCIAL_IDP_ALIAS, fid);
        assertEquals(1, testUserResource().getFederatedIdentity().size());
        linkedAccountsPage.navigateTo();

        assertProvider(systemIdp, false, false, "");
        assertProvider(socialIdp, true, true, homerUser.getUsername());

        socialIdp.clickUnlinkBtn();
        linkedAccountsPage.assertCurrent();
        assertProvider(systemIdp, false, false, "");
        assertProvider(socialIdp, false, true, "");

        assertEquals(0, testUserResource().getFederatedIdentity().size());
    }

    private void assertProvider(
            LinkedAccountsPage.IdentityProvider provider,
            boolean expectLinked,
            boolean expectSocial,
            String expectedUsername
    ) {
        if (expectLinked) {
            assertTrue("Account should be in the \"Linked\" list", provider.isLinked());
            assertTrue("Unlink button should be visible", provider.isUnlinkBtnVisible());
            assertFalse("Link button shouldn't be visible", provider.isLinkBtnVisible());
        }
        else {
            assertFalse("Account should be in the \"Unlinked\" list", provider.isLinked());
            assertFalse("Unlink button shouldn't be visible", provider.isUnlinkBtnVisible());
            assertTrue("Link button should be visible", provider.isLinkBtnVisible());
        }

        if (expectSocial) {
            assertTrue("Social badge should be visible", provider.hasSocialLoginBadge());
            assertTrue("Social icon should be visible", provider.hasSocialIcon());
            assertFalse("Default icon shouldn't be visible", provider.hasDefaultIcon());
        }
        else {
            assertFalse("Social badge shouldn't be visible", provider.hasSocialLoginBadge());
            assertFalse("Social icon shouldn't be visible", provider.hasSocialIcon());
            assertTrue("Default icon should be visible", provider.hasDefaultIcon());
        }

        assertEquals(expectedUsername, provider.getUsername());
    }

    private void assertProvidersCount() {
        assertEquals(2,
                linkedAccountsPage.getLinkedProvidersCount() + linkedAccountsPage.getUnlinkedProvidersCount());
    }
}
