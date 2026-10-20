/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.client.policies;

import org.keycloak.OAuthErrorException;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.RejectImplicitGrantExecutor;
import org.keycloak.services.clientpolicy.executor.RejectImplicitGrantExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * RejectImplicitGrantExecutor must treat a missing implicitFlowEnabled as
 * disabled: the field is optional in ClientRepresentation and Keycloak
 * defaults it to disabled, so omitting it must not fail the validation
 * with a NullPointerException when the executor runs with
 * auto-configure disabled.
 */
@KeycloakIntegrationTest
public class RejectImplicitGrantExecutorTest extends AbstractClientPoliciesTest {

    @InjectRealm
    ManagedRealm realm;

    private void setupRejectImplicitGrantPolicy() throws Exception {
        RejectImplicitGrantExecutor.Configuration config = new RejectImplicitGrantExecutor.Configuration();
        config.setAutoConfigure(false);
        setupPolicy(realm, RejectImplicitGrantExecutorFactory.PROVIDER_ID, config,
                AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation());
    }

    @Test
    public void clientCreateWithoutImplicitFlowEnabledSucceeds() throws Exception {
        setupRejectImplicitGrantPolicy();

        // The field is optional: a client that omits it is created and the
        // implicit flow remains disabled.
        createClientByAdmin(realm, "implicit-omitted-client", OIDCLoginProtocol.LOGIN_PROTOCOL, r -> { });
        Assertions.assertFalse(findByClientIdByAdmin(realm, "implicit-omitted-client").isImplicitFlowEnabled());
    }

    @Test
    public void clientCreateWithImplicitFlowEnabledRejected() throws Exception {
        setupRejectImplicitGrantPolicy();

        ClientPolicyException e = Assertions.assertThrows(ClientPolicyException.class,
                () -> createClientByAdmin(realm, "implicit-enabled-client", OIDCLoginProtocol.LOGIN_PROTOCOL, r -> r.setImplicitFlowEnabled(true)));
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
    }

    @Test
    public void clientUpdateOmittingImplicitFlowEnabledSucceeds() throws Exception {
        setupRejectImplicitGrantPolicy();

        String cId = createClientByAdmin(realm, "implicit-update-client", OIDCLoginProtocol.LOGIN_PROTOCOL, r -> { });
        // An update that omits the field leaves the setting unchanged and
        // must not fail the validation.
        updateClientByAdmin(realm, cId, r -> r.setImplicitFlowEnabled(null));
        Assertions.assertFalse(findByClientIdByAdmin(realm, "implicit-update-client").isImplicitFlowEnabled());
    }

    @Test
    public void clientUpdateEnablingImplicitFlowRejected() throws Exception {
        setupRejectImplicitGrantPolicy();

        String cId = createClientByAdmin(realm, "implicit-enable-update-client", OIDCLoginProtocol.LOGIN_PROTOCOL, r -> { });
        ClientPolicyException e = Assertions.assertThrows(ClientPolicyException.class,
                () -> updateClientByAdmin(realm, cId, r -> r.setImplicitFlowEnabled(true)));
        Assertions.assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getError());
    }
}
