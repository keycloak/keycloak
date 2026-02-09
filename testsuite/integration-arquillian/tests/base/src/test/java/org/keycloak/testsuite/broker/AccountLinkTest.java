/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.broker;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.PassThroughFederatedUserStorageProvider;
import org.keycloak.testsuite.federation.PassThroughFederatedUserStorageProviderFactory;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FederatedIdentityBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AccountLinkTest extends AbstractKeycloakTest {
    public static final String CHILD_IDP = "child";
    public static final String PARENT_IDP = "parent-idp";
    public static final String PARENT_USERNAME = "parent";

    @Page
    protected UpdateAccountInformationPage profilePage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(CHILD_IDP);
        realm.setEnabled(true);
        testRealms.add(realm);

        realm = new RealmRepresentation();
        realm.setRealm(PARENT_IDP);
        realm.setEnabled(true);

        testRealms.add(realm);

    }

    @Before
    public void beforeBrokerTest() {
        if (testContext.isInitialized()) {
            return;
        }

        // addIdpUser
        RealmResource realmParent = adminClient.realms().realm(PARENT_IDP);
        UserRepresentation user = new UserRepresentation();
        user.setUsername(PARENT_USERNAME);
        user.setEnabled(true);
        String userId = createUserAndResetPasswordWithAdminClient(realmParent, user, "password");

        // addChildUser
        RealmResource realmChild = adminClient.realms().realm(CHILD_IDP);
        user = new UserRepresentation();
        user.setUsername("child");
        user.setEnabled(true);
        userId = createUserAndResetPasswordWithAdminClient(realmChild, user, "password");

        // setupUserStorageProvider
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("passthrough");
        provider.setProviderId(PassThroughFederatedUserStorageProviderFactory.PROVIDER_ID);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle("priority", Integer.toString(1));
        realmChild.components().add(provider);

        // createBroker
        createParentChild();

        testContext.setInitialized(true);
    }


    public void createParentChild() {
        BrokerTestTools.createKcOidcBroker(adminClient, CHILD_IDP, PARENT_IDP);
    }

    @Test
    public void testAccountLink() {
        String childUsername = "child";

        testAccountLink(childUsername);
    }

    @Test
    @Ignore // Ignore should be removed by https://github.com/keycloak/keycloak/issues/20441
    public void testAccountLinkWithUserStorageProvider() {

        String childUsername = PassThroughFederatedUserStorageProvider.PASSTHROUGH_USERNAME;
        String childPassword = PassThroughFederatedUserStorageProvider.INITIAL_PASSWORD;
        String childIdp = CHILD_IDP;

        testAccountLink(childUsername);
    }

    @Test
    public void testDeleteIdentityOnProviderRemoval() {
        String childUsername = "child";

        assertFederatedIdentity(childUsername);

        RealmResource realm = adminClient.realm(CHILD_IDP);
        UsersResource users = realm.users();
        List<UserRepresentation> search = users.search(childUsername);
        assertFalse(search.isEmpty());
        String userId = search.get(0).getId();
        List<FederatedIdentityRepresentation> identities = users.get(userId).getFederatedIdentity();
        assertFalse(identities.isEmpty());

        realm.identityProviders().get(PARENT_IDP).remove();
        
        identities = users.get(userId).getFederatedIdentity();
        assertTrue(identities.isEmpty());

        getTestingClient().server(CHILD_IDP).run(AccountLinkTest::checkEmptyFederatedIdentities);
    }

    @Test
    public void testDeleteFederatedUserFederatedIdentityOnProviderRemoval() {
        RealmResource realm = adminClient.realm(CHILD_IDP);
        final String testIdpToDelete = "test-idp-to-delete";

        BrokerTestTools.createKcOidcBroker(adminClient, CHILD_IDP, testIdpToDelete);

        // Create user federation

        ComponentRepresentation memProvider = new ComponentRepresentation();
        memProvider.setName("memory");
        memProvider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        memProvider.setProviderType(UserStorageProvider.class.getName());
        memProvider.setConfig(new MultivaluedHashMap<>());
        memProvider.getConfig().putSingle("priority", Integer.toString(0));
        memProvider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));

        Response resp = realm.components().add(memProvider);
        resp.close();
        String memProviderId = ApiUtil.getCreatedId(resp);

        // Create federated user
        String username = "fed-user1";
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(username);
        userRepresentation.setEmail("feduser1@mail.com");
        userRepresentation.setRequiredActions(Collections.emptyList());
        userRepresentation.setEnabled(true);
        userRepresentation.setFederationLink(memProviderId);
        String userId = createUserWithAdminClient(realm, userRepresentation);
        Assert.assertFalse(StorageId.isLocalStorage(userId));

        // Link identity provider and federated user
        FederatedIdentityRepresentation identity = FederatedIdentityBuilder.create()
            .userId(userId)
            .userName(username)
            .identityProvider(testIdpToDelete)
            .build();

        UserResource userResource = realm.users().get(userId);
        Response response = userResource.addFederatedIdentity(testIdpToDelete, identity);
        Assert.assertEquals("status", 204, response.getStatus());

        userResource = realm.users().get(userId);
        Assert.assertFalse(userResource.getFederatedIdentity().isEmpty());

        // Delete the identity provider
        realm.identityProviders().get(testIdpToDelete).remove();

        // Check that links to federated identity has been deleted
        userResource = realm.users().get(userId);
        Assert.assertTrue(userResource.getFederatedIdentity().isEmpty());

        getTestingClient().server(CHILD_IDP).run((RunOnServer) session -> {
            RealmModel realm1 = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm1, username);
            assertEquals(0, session.users().getFederatedIdentitiesStream(realm1, user).count());
            assertNull(session.users().getFederatedIdentity(realm1, user, testIdpToDelete));
        });
    }

    private static void checkEmptyFederatedIdentities(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserByUsername(realm, "child");
        assertEquals(0, session.users().getFederatedIdentitiesStream(realm, user).count());
        assertNull(session.users().getFederatedIdentity(realm, user, PARENT_IDP));
    }

    protected void testAccountLink(String childUsername) {
        assertFederatedIdentity(childUsername);
        assertRemoveFederatedIdentity();

    }

    private void assertFederatedIdentity(String childUsername) {
        //Link the identity provider through Admin REST API
        Response response = AccountHelper.addIdentityProvider(adminClient.realm(CHILD_IDP), childUsername, adminClient.realm(PARENT_IDP), PARENT_USERNAME, PARENT_IDP);
        Assert.assertEquals("status", 204, response.getStatus());
        assertTrue(AccountHelper.isIdentityProviderLinked(adminClient.realm(CHILD_IDP), childUsername, PARENT_IDP));

    }

    private void assertRemoveFederatedIdentity() {
        // Unlink my "test-user" through Admin REST API
        AccountHelper.deleteIdentityProvider(adminClient.realm(CHILD_IDP), CHILD_IDP, PARENT_IDP);
        assertFalse(AccountHelper.isIdentityProviderLinked(adminClient.realm(CHILD_IDP), CHILD_IDP, PARENT_IDP));

    }

}
