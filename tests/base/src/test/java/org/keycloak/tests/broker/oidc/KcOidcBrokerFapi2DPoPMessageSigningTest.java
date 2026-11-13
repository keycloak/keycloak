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
package org.keycloak.tests.broker.oidc;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Brokers with signed request objects to another realm that enforces the FAPI 2.0 Message Signing profile with DPoP
 * on its clients.
 */
@KeycloakIntegrationTest(config = AbstractKcOidcBrokerFapi2Test.Fapi2ServerConfig.class)
public class KcOidcBrokerFapi2DPoPMessageSigningTest extends AbstractKcOidcBrokerFapi2Test {

    @Override
    protected String getClientProfile() {
        return "fapi-2-dpop-message-signing";
    }

    @BeforeEach
    void enableRequestObject() {
        updateIdentityProvider(config -> {
            config.put(OAuth2IdentityProviderConfig.REQUEST_OBJECT_ENABLED, "true");
            config.put(OAuth2IdentityProviderConfig.REQUEST_OBJECT_SIGNING_ALG, SIGN_ALG);
        });
    }

    @Test
    public void testRequestObjectLifespanIsCapped() {
        RealmRepresentation consumer = consumerRealm.admin().toRepresentation();
        consumer.setAccessCodeLifespan(7200);
        consumerRealm.admin().update(consumer);

        // the provider rejects request objects valid for more than 60 minutes
        loginUser();
    }

    @Test
    public void testUnsignedRequestIsRejected() {
        updateIdentityProvider(config -> config.put(OAuth2IdentityProviderConfig.REQUEST_OBJECT_ENABLED, "false"));

        startLogin(false);

        // the provider redirects the error back and the consumer shows it on its login page
        loginPage.assertCurrent();
        EventAssertion.assertError(providerEvents.poll())
                .type(EventType.LOGIN_ERROR)
                .error(Errors.INVALID_REQUEST)
                .details(Details.REASON, Details.CLIENT_POLICY_ERROR);
        Assertions.assertNull(providerEvents.poll());
    }
}
