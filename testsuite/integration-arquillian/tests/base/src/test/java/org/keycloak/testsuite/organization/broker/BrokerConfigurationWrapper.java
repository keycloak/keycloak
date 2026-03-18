/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.organization.broker;

import java.util.List;

import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.broker.BrokerConfiguration;
import org.keycloak.testsuite.util.UserBuilder;

import static org.keycloak.testsuite.AbstractTestRealmKeycloakTest.TEST_REALM_NAME;

public class BrokerConfigurationWrapper implements BrokerConfiguration {

    private final String orgName;
    private final BrokerConfiguration delegate;

    public BrokerConfigurationWrapper(String orgName, BrokerConfiguration delegate) {
        this.orgName = orgName;
        this.delegate = delegate;
    }

    @Override
    public String consumerRealmName() {
        return TEST_REALM_NAME;
    }

    @Override
    public RealmRepresentation createProviderRealm() {
        RealmRepresentation providerRealm = delegate.createProviderRealm();

        providerRealm.setClients(createProviderClients());
        providerRealm.setUsers(List.of(
                        UserBuilder.create()
                                .username(getUserLogin())
                                .email(getUserEmail())
                                .password(getUserPassword())
                                .enabled(true)
                                .build(),
                        UserBuilder.create()
                                .username("external")
                                .email("external@user.org")
                                .password("password")
                                .enabled(true)
                                .build()
                )
        );

        return providerRealm;
    }

    @Override
    public String getUserEmail() {
        return getUserLogin() + "@" + orgName + ".org";
    }

    @Override
    public String getIDPAlias() {
        return orgName + "-identity-provider";
    }

    @Override
    public IdentityProviderRepresentation setUpIdentityProvider() {
        IdentityProviderRepresentation broker = delegate.setUpIdentityProvider();
        broker.setAlias(getIDPAlias());
        // by default set the test org idps as not available for login pages.
        broker.setHideOnLogin(true);
        return broker;
    }

    @Override
    public List<ClientRepresentation> createProviderClients() {
        List<ClientRepresentation> clients = delegate.createProviderClients();
        clients.get(0).setRedirectUris(List.of("*"));
        return clients;
    }

    @Override
    public RealmRepresentation createConsumerRealm() {
        return delegate.createConsumerRealm();
    }

    @Override
    public List<ClientRepresentation> createConsumerClients() {
        return delegate.createConsumerClients();
    }

    @Override
    public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode force) {
        IdentityProviderRepresentation broker = delegate.setUpIdentityProvider(force);
        broker.setAlias(getIDPAlias());
        return broker;
    }

    @Override
    public String providerRealmName() {
        return delegate.providerRealmName();
    }

    @Override
    public String getIDPClientIdInProviderRealm() {
        return delegate.getIDPClientIdInProviderRealm();
    }

    @Override
    public String getUserLogin() {
        return delegate.getUserLogin();
    }

    @Override
    public String getUserPassword() {
        return delegate.getUserPassword();
    }
}
