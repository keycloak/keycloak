/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.oauth;

import java.io.Serializable;
import java.util.Map;
import java.util.function.BiFunction;

import org.keycloak.OAuth2Constants;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.runonserver.FetchOnServer;
import org.keycloak.testsuite.runonserver.FetchOnServerWrapper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for simulating token refresh with the offline tokens created in older Keycloak versions.
 *
 * Keycloak supports refresh of the offline tokens, which were created in older Keycloak versions than current Keycloak version. But
 * testing real migration is sometimes hard to achieve as it requires running of the old Keycloak server, which is sometimes not feasible.
 *
 * This test just simulates the refresh with the old offline-token by manually converting offline-token to the offline-token format, which was used by the specified old Keycloak version
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineTokenMigrationTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginPage loginPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    // Issue 31224
    // Test refresh with the offline-token created in Keycloak 14 works
    @Test
    public void testOfflineTokenMigrationFromKeycloak14() throws Exception {
        OfflineTokenConverter convertOfflineTokenToKeycloak14Format = (session, oldOfflineToken) -> {
            try {
                RefreshToken refreshToken = session.tokens().decode(oldOfflineToken, RefreshToken.class);
                String sessionId = refreshToken.getSessionId();
                String signatureAlgorithm = new JWSInput(oldOfflineToken).getHeader().getAlgorithm().toString();

                Map<String, String> asMap = JsonSerialization.readValue(JsonSerialization.writeValueAsString(refreshToken), Map.class);
                asMap.remove(IDToken.SESSION_ID);
                asMap.put(IDToken.SESSION_STATE, sessionId);

                SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
                SignatureSignerContext signer = signatureProvider.signer();

                String type = "JWT";
                return new JWSBuilder().type(type).jsonContent(asMap).sign(signer);
            } catch (Exception ioe) {
                throw new RuntimeException(ioe);
            }
        };

        testOfflineTokenMigration(convertOfflineTokenToKeycloak14Format);
    }

    private void testOfflineTokenMigration(OfflineTokenConverter offlineTokenConverter) throws Exception {
        // Send request to obtain offline token
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("direct-grant", "password");

        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        Assert.assertNull(tokenResponse.getErrorDescription());
        String offlineTokenString = tokenResponse.getRefreshToken();

        // Convert offline token to the format of some old Keycloak version
        FetchOnServerWrapper<String> fetch = new FetchOnServerWrapper<>() {

            @Override
            public FetchOnServer getRunOnServer() {
                return session -> offlineTokenConverter.apply(session, offlineTokenString);
            }

            @Override
            public Class<String> getResultClass() {
                return String.class;
            }

        };
        String modifiedOfflineToken = testingClient.server("test").fetch(fetch);
        getLogger().infof("Modified offline token: %s", modifiedOfflineToken);

        // Check it is possible to successfully refresh with the modified offline token
        AccessTokenResponse response = oauth.doRefreshTokenRequest(modifiedOfflineToken);
        AccessToken refreshedToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(200, response.getStatusCode());
    }

    public interface OfflineTokenConverter extends Serializable, BiFunction<KeycloakSession, String, String> {
    }
}
