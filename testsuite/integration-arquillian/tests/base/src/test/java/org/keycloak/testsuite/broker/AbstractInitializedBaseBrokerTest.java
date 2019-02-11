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
package org.keycloak.testsuite.broker;

import org.junit.Before;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import java.util.List;
import static org.keycloak.testsuite.admin.ApiUtil.createUserWithAdminClient;
import static org.keycloak.testsuite.admin.ApiUtil.resetUserPassword;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 */
public abstract class AbstractInitializedBaseBrokerTest extends AbstractBaseBrokerTest {

    protected IdentityProviderResource identityProviderResource;

    @Before
    public void beforeBrokerTest() {
        log.debug("creating user for realm " + bc.providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(bc.getUserLogin());
        user.setEmail(bc.getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        RealmResource realmResource = adminClient.realm(bc.providerRealmName());
        userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), bc.getUserPassword(), false);

        if (testContext.isInitialized()) {
            if (identityProviderResource == null) {
                identityProviderResource = (IdentityProviderResource) testContext.getCustomValue("identityProviderResource");
            }
            return;
        }

        log.debug("adding identity provider to realm " + bc.consumerRealmName());
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        realm.identityProviders().create(bc.setUpIdentityProvider(suiteContext)).close();
        identityProviderResource = realm.identityProviders().get(bc.getIDPAlias());
        testContext.setCustomValue("identityProviderResource", identityProviderResource);

        // addClients
        List<ClientRepresentation> clients = bc.createProviderClients(suiteContext);
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getClientId()+ " to realm " + bc.providerRealmName());

                providerRealm.clients().create(client).close();
            }
        }

        clients = bc.createConsumerClients(suiteContext);
        if (clients != null) {
            RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getClientId() + " to realm " + bc.consumerRealmName());

                consumerRealm.clients().create(client).close();
            }
        }

        testContext.setInitialized(true);
    }

}