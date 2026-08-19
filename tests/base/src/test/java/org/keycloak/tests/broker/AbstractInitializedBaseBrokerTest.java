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
package org.keycloak.tests.broker;

import java.util.function.BiConsumer;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.tests.utils.admin.AdminApiUtil.createUserWithAdminClient;
import static org.keycloak.tests.utils.admin.AdminApiUtil.resetUserPassword;

/**
 * @author Stan Silvert ssilvert@redhat.com (C) 2019 Red Hat Inc.
 */
@KeycloakIntegrationTest(config = org.keycloak.tests.broker.BrokerServerConfig.class)
public abstract class AbstractInitializedBaseBrokerTest extends AbstractBaseBrokerTest {

    @InjectRealm
    ManagedRealm managedRealm;

    protected IdentityProviderResource identityProviderResource;

    protected void postInitializeUser(UserRepresentation user) {}
    
    @Override
    @BeforeEach
    public void beforeBrokerTest() {
        super.beforeBrokerTest();
        log.debug("creating user for realm " + bc.providerRealmName());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(bc.getUserLogin());
        user.setEmail(bc.getUserEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);
        postInitializeUser(user);

        RealmResource realmResource = adminClient.realm(bc.providerRealmName());
        userId = createUserWithAdminClient(realmResource, user);

        resetUserPassword(realmResource.users().get(userId), bc.getUserPassword(), false);

        log.debug("adding identity provider to realm " + bc.consumerRealmName());
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        Response response = realm.identityProviders().create(bc.setUpIdentityProvider());
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String error = null;
            try {
                error = response.readEntity(String.class);
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("Failed to add identity provider " + bc.getIDPAlias() + " to realm " + bc.consumerRealmName()
                    + ": " + response.getStatus() + (error != null && !error.isBlank() ? " - " + error : ""));
        }
        response.close();
        identityProviderResource = realm.identityProviders().get(bc.getIDPAlias());

        addClientsToProviderAndConsumer();

        testContext.setInitialized(true);
    }

    protected void updateExecutions(BiConsumer<AuthenticationExecutionInfoRepresentation, AuthenticationManagementResource> action) {
        AuthenticationManagementResource flows = adminClient.realm(bc.consumerRealmName()).flows();

        for (AuthenticationExecutionInfoRepresentation execution : flows.getExecutions(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW)) {
            action.accept(execution, flows);
        }
    }

}
