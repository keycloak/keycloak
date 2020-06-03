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
package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.AbstractBaseBrokerTest;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.broker.KcOidcBrokerConfiguration;
import org.keycloak.testsuite.pages.AccountFederatedIdentityPage;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.Response;

import java.util.List;

import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class AccountBrokerTest extends AbstractBaseBrokerTest {

    @Page
    protected AccountFederatedIdentityPage identityPage;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Before
    public void createUser() {
        log.debug("creating user for realm " + bc.providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(bc.getUserLogin());
        user.setEmail(bc.getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(bc.providerRealmName());
        userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), bc.getUserPassword(), false);
    }

    @Before
    public void addIdentityProviderToProviderRealm() {
        log.debug("adding identity provider to realm " + bc.consumerRealmName());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().create(bc.setUpIdentityProvider()).close();
        realm.identityProviders().get(bc.getIDPAlias());
    }

    @Before
    public void addClients() {
        List<ClientRepresentation> clients = bc.createProviderClients();
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + bc.providerRealmName());

                // Remove default client scopes for this test
//                client.setDefaultClientScopes(Collections.emptyList());

                fixAuthServerHostAndPortForClientRepresentation(client);

                providerRealm.clients().create(client).close();
            }
        }

        clients = bc.createConsumerClients();
        if (clients != null) {
            RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + bc.consumerRealmName());

                fixAuthServerHostAndPortForClientRepresentation(client);

                consumerRealm.clients().create(client).close();
            }
        }
    }

    @Before
    public void before() {
        Response response = adminClient.realm(KcOidcBrokerConfiguration.INSTANCE.consumerRealmName()).users().create(UserBuilder.create().username("accountbrokertest").build());
        String userId = ApiUtil.getCreatedId(response);
        ApiUtil.resetUserPassword(adminClient.realm(KcOidcBrokerConfiguration.INSTANCE.consumerRealmName()).users().get(userId), "password", false);
    }

    @After
    public void after() {
        RealmResource consumerRealm = adminClient.realm(KcOidcBrokerConfiguration.INSTANCE.consumerRealmName());
        UserRepresentation userRep = ApiUtil.findUserByUsername(consumerRealm, "accountbrokertest");
        if (userRep != null) {
            consumerRealm.users().get(userRep.getId()).remove();
        }
    }

    @Test
    public void add() {
        identityPage.realm(KcOidcBrokerConfiguration.INSTANCE.consumerRealmName());
        identityPage.open();
        loginPage.login("accountbrokertest", "password");
        Assert.assertTrue(identityPage.isCurrent());

        List<AccountFederatedIdentityPage.FederatedIdentity> identities = identityPage.getIdentities();
        Assert.assertEquals(1, identities.size());

        Assert.assertEquals("kc-oidc-idp", identities.get(0).getProvider());
        Assert.assertEquals("", identities.get(0).getSubject());
        Assert.assertEquals("add-link-kc-oidc-idp", identities.get(0).getAction().getAttribute("id"));

        identities.get(0).getAction().click();

        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        Assert.assertTrue(identityPage.isCurrent());

        identities = identityPage.getIdentities();
        Assert.assertEquals(1, identities.size());

        Assert.assertEquals("kc-oidc-idp", identities.get(0).getProvider());
        Assert.assertEquals("testuser", identities.get(0).getSubject());
        Assert.assertEquals("remove-link-kc-oidc-idp", identities.get(0).getAction().getAttribute("id"));

        identities.get(0).getAction().click();

        Assert.assertTrue(identityPage.isCurrent());

        identities = identityPage.getIdentities();

        Assert.assertEquals("kc-oidc-idp", identities.get(0).getProvider());
        Assert.assertEquals("", identities.get(0).getSubject());
        Assert.assertEquals("add-link-kc-oidc-idp", identities.get(0).getAction().getAttribute("id"));
    }

    @Test
    public void displayEnabledIdentityProviders() {
        identityPage.realm(KcOidcBrokerConfiguration.INSTANCE.consumerRealmName());
        identityPage.open();
        loginPage.login("accountbrokertest", "password");
        Assert.assertTrue(identityPage.isCurrent());

        List<AccountFederatedIdentityPage.FederatedIdentity> identities = identityPage.getIdentities();
        Assert.assertEquals(1, identities.size());

        // Disable the identity provider
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        IdentityProviderResource providerResource = realm.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation provider = providerResource.toRepresentation();
        provider.setEnabled(false);
        providerResource.update(provider);

        // Reload federated identities page
        identityPage.open();
        Assert.assertTrue(identityPage.isCurrent());

        identities = identityPage.getIdentities();
        Assert.assertEquals(0, identities.size());
    }

}
