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

package org.keycloak.testsuite.client;


import java.util.Collections;

import org.keycloak.client.registration.Auth;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.TokenSignatureUtil;

import org.junit.Before;
import org.junit.Test;

public class OIDCClientRegistrationDefaultSignatureTest extends AbstractClientRegistrationTest {
    @Before
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    private OIDCClientRepresentation createRep() {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("RegistrationAccessTokenTest");
        client.setClientUri("http://root");
        client.setRedirectUris(Collections.singletonList("http://redirect"));
        client.setFrontChannelLogoutUri("http://frontchannel");
        client.setFrontchannelLogoutSessionRequired(true);
        return client;
    }

    @Test
    public void testIdTokenSignedResponse() throws Exception {
        OIDCClientRepresentation response = null;
        OIDCClientRepresentation updated = null;
        try {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.ES512);

            // create (no specification)
            OIDCClientRepresentation clientRep = createRep();

            response = reg.oidc().create(clientRep);
            Assert.assertEquals(Algorithm.ES512, response.getIdTokenSignedResponseAlg());

            // Test Keycloak representation
            ClientRepresentation kcClient = getClient(response.getClientId());
            OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            // Client representation of id.token.signed.response.alg is null as from realm
            Assert.assertNull(config.getIdTokenSignedResponseAlg());

            // update
            reg.auth(Auth.token(response));
            response.setIdTokenSignedResponseAlg(Algorithm.ES256);
            updated = reg.oidc().update(response);
            Assert.assertEquals(Algorithm.ES256, updated.getIdTokenSignedResponseAlg());

            // Test Keycloak representation
            kcClient = getClient(updated.getClientId());
            config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            Assert.assertEquals(Algorithm.ES256, config.getIdTokenSignedResponseAlg());

            // update after changing default realm token signature
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.ES384);
            reg.auth(Auth.token(updated));
            updated.setIdTokenSignedResponseAlg(null);
            response = reg.oidc().update(updated);
            Assert.assertEquals(Algorithm.ES384, response.getIdTokenSignedResponseAlg());
        } finally {
            // revert
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
            reg.auth(Auth.token(response));
            response.setIdTokenSignedResponseAlg(null);
            updated = reg.oidc().update(response);
            Assert.assertNull(updated.getIdTokenSignedResponseAlg());

            // Test Keycloak representation
            ClientRepresentation kcClient = getClient(updated.getClientId());
            OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientRepresentation(kcClient);
            // Client representation of id.token.signed.response.alg is null as from realm
            Assert.assertNull(config.getIdTokenSignedResponseAlg());
        }
    }
}
